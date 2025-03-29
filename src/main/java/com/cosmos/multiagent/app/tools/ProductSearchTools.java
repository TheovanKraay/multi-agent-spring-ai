package com.cosmos.multiagent.app.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

public class ProductSearchTools {

    private VectorStore vectorStore;
    public ProductSearchTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search for a product in the vector store by text query")
    String productSearch(String searchText) {
        System.out.println("In ProductSearchTools");
        List<Document> results = this.vectorStore.similaritySearch(SearchRequest.builder().query(searchText).topK(3).build());
        results.forEach(result -> {
            var id = result.getId();
            System.out.println("id: " + id);
        });
        if (results.isEmpty()) {
            System.out.println("No results found.");
            return "No results found.";
        }
        System.out.println("Results: " + results);
        return results.toString();
    }
}
