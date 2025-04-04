package com.cosmos.multiagent.repository;

import com.azure.spring.data.cosmos.core.mapping.Container;
import org.springframework.data.annotation.Id;

@Container
public class Products {
    // Constructors
    public Products() {}

    @Id
    private String id;

    private String content;

    private Metadata metadata;



/*    public Products(String id, String content, Metadata metadata) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
    }*/

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    // Nested metadata class
    public static class Metadata {

        private String productId;
        private String price;
        private String category;
        private String productName;

        public Metadata() {}

        public Metadata(String productId, String price, String category, String productName) {
            this.productId = productId;
            this.price = price;
            this.category = category;
            this.productName = productName;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
    }
}

