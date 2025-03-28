package com.cosmos.multiagent.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataLoadRunner implements CommandLineRunner {

    private final VectorStore vectorStore;

    public DataLoadRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = new ClassPathResource("data/products.json").getInputStream();

        List<Map<String, Object>> products = mapper.readValue(inputStream, new TypeReference<>() {});
        List<Document> documents = new ArrayList<>();

        for (Map<String, Object> product : products) {
            StringBuilder content = new StringBuilder();
            content.append("Product ID: ").append(product.get("product_id")).append("\n");
            content.append("Product Name: ").append(product.get("product_name")).append("\n");
            content.append("Category: ").append(product.get("category")).append("\n");
            content.append("Description: ").append(product.get("product_description")).append("\n");
            content.append("Price: ").append(product.get("price"));

            Document doc = new Document(content.toString());
            doc.getMetadata().put("product_id", product.get("product_id").toString());
            doc.getMetadata().put("product_name", product.get("product_name").toString());
            documents.add(doc);
        }

        vectorStore.add(documents);
        System.out.println("✅ Loaded " + documents.size() + " products into the vector store.");
    }
}
