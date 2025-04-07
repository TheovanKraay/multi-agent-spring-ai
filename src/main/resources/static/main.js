// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Inject v-cloak CSS to prevent {{ }} from showing before Vue mounts
const cloakStyle = document.createElement('style');
cloakStyle.innerHTML = '[v-cloak] { display: none; }';
document.head.appendChild(cloakStyle);

const CONTEXT_MESSAGE_COUNT = 5;
const DEFAULT_GREETING_MESSAGE = "Hello! How can I assist you today?";
const API_URL = '/api/chat';
const API_HEADER = { "Content-Type": "application/json" };
const TENANT_ID = "default";

var app = new Vue({
  el: '#main',
  data: {
    chatHistory: {
      data: [],
      activeChatId: null,
      job: {}
    },
    userId: null
  },
  created: async function () {
    console.log("Vue app created");
    await this.initUserId();
    await this.loadSessions();
  },
  methods: {
    parseMarkdown: marked.parse,
    scrollThreadToBottom: function () {
      const chatMessageList = document.getElementById("chat-thread-message-list");
      if (chatMessageList) chatMessageList.scrollTop = chatMessageList.scrollHeight;
    },
    scrollHistoryToTop: function () {
      const chatHistoryList = document.getElementById("chat-history-list");
      if (chatHistoryList) chatHistoryList.scrollTop = 0;
    },
    parseTimestamp: function (timestamp) {
      const day = new Date(timestamp);
      const date = `${day.getMonth() + 1}/${day.getDate()}/${day.getFullYear()}`;
      const time = day.toLocaleTimeString('en-US', { hour12: true, hour: 'numeric', minute: 'numeric', second: 'numeric' });
      return `${date}, ${time}`;
    },
    initUserId: async function () {
      try {
        const res = await fetch('https://api.ipify.org?format=json');
        const data = await res.json();
        this.userId = data.ip.replace(/\./g, '-');
      } catch {
        this.userId = "anonymous";
      }
    },
    loadSessions: async function () {
      const res = await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/sessions`);
      if (!res.ok) return;

      const sessions = await res.json();
      for (let session of sessions) {
        const messageRes = await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/session/${session.sessionId}/messages?lastN=50`);
        const messages = messageRes.ok ? await messageRes.json() : [];

        this.chatHistory.data.push({
          id: session.sessionId,
          title: session.name || "New chat",
          timestamp: Date.now(),
          messages: Array.isArray(messages) ? messages.map(m => ({
              sender: m.role === 'user' ? 'human' : 'bot',
              text: m.role !== 'user' ? `**${m.role} Agent**: ${m.text}` : m.text,
              timestamp: Date.now()
            })) : []
        });
      }

      if (this.chatHistory.data.length > 0) {
        this.openChat(this.chatHistory.data[this.chatHistory.data.length - 1].id);
      } else {
        this.newChat();
      }
    },
    openChat: function (id) {
      this.chatHistory.activeChatId = id;
      setTimeout(() => this.scrollThreadToBottom(), 0);
    },
    newChat: async function () {
      const res = await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/session`, {
        method: "POST",
        headers: API_HEADER
      });
      const sessionId = await res.text();
      const timestamp = Date.now();
      const newChat = {
        id: sessionId,
        title: "New chat",
        timestamp: timestamp,
        messages: [{
          sender: "bot",
          text: DEFAULT_GREETING_MESSAGE,
          timestamp: timestamp
        }]
      };
      this.chatHistory.data.push(newChat);
      this.chatHistory.activeChatId = sessionId;
      this.scrollHistoryToTop();
      this.scrollThreadToBottom();
    },
    sendMessage: async function () {
      const message = document.getElementById("chat-input-box").value.trim();
      if (!message) return;

      document.getElementById("chat-input-box").value = "";

      const chat = this.chatHistory.data.find(chat => chat.id === this.chatHistory.activeChatId);
      const timestamp = Date.now();

      chat.messages.push({
        sender: "human",
        text: message,
        timestamp: timestamp
      });

      if (chat.title === "New chat") {
        chat.title = message;
      }

      this.addJob(chat.id, message);
      setTimeout(() => this.scrollThreadToBottom(), 0);
    },
    addJob: async function (id, userInput) {
      this.chatHistory.job[id] = this.chatHistory.job[id] ?? [];
      this.chatHistory.job[id].push("processing");

      await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/session/${id}/completion?input=${encodeURIComponent(userInput)}`, {
        method: "POST",
        headers: API_HEADER
      });

      try {
        const res = await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/session/${id}/messages?lastN=50`);
        if (res.ok) {
          const messages = await res.json();
          const chat = this.chatHistory.data.find(chat => chat.id === id);
          if (chat && messages && Array.isArray(messages)) {
            chat.messages = messages.map(m => ({
              sender: m.role === 'user' ? 'human' : 'bot',
              text: m.role !== 'user' ? `**${m.role} Agent**: ${m.text}` : m.text,
              timestamp: Date.now()
            }));
            this.scrollThreadToBottom();
          } else {
            console.warn("No chat or messages returned");
          }
        } else {
          console.error("Failed to load messages from backend.");
        }
      } catch (err) {
        console.error("Error fetching messages:", err);
      } finally {
        this.chatHistory.job[id] = [];
      }
    },
    pressToSendMessage: function (e) {
      if (e && e.key === "Enter") {
        this.sendMessage();
      }
    },

    removeChat: async function (id) {
      try {
        await fetch(`${API_URL}/tenant/${TENANT_ID}/user/${this.userId}/session/${id}`, {
          method: "DELETE",
          headers: API_HEADER
        });
    
        // Update chat history locally
        this.chatHistory.data = this.chatHistory.data.filter(chat => chat.id !== id);
    
        if (this.chatHistory.activeChatId === id) {
          this.chatHistory.activeChatId = this.chatHistory.data.length
            ? this.chatHistory.data[0].id
            : null;
        }
    
        // Wait a moment, then reload the page so the UI refreshes
        setTimeout(() => {
          window.location.reload();
        }, 100);
    
      } catch (err) {
        console.warn("Failed to delete session:", err);
      }
    },
    
  }
});
