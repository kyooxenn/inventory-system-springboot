package com.java.inventory.system;

import com.github.javafaker.Faker;
import com.java.inventory.system.util.ProductIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Locale;

@Component
@Slf4j
public class ProductSeeder implements CommandLineRunner {

    @Value("${DB_URL}")
    private String url;

    @Value("${DB_USERNAME}")
    private String user;

    @Value("${DB_PASSWORD}")
    private String password;

    @Override
    public void run(String... args) throws Exception {
        log.info("🌱 Checking product table before seeding...");

        Faker faker = new Faker(new Locale("en"));

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            // ✅ Check if table already has data
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM product")) {

                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    log.warn("⚠️ Product table already has {} rows. Skipping seeding.", count);
                    return;
                }
            }

            // ✅ Proceed with seeding
            log.info("🌱 Seeding 1000 products into database...");
            String sql = "INSERT INTO product (id, category, description, item_name, quantity, unit, unit_price) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < 1000; i++) {
                    ps.setString(1, ProductIdGenerator.generateId());
                    ps.setString(2, faker.options().option(
                            "Books", "Movies", "Music", "Games", "Electronics", "Computers",
                            "Home", "Garden", "Tools", "Grocery", "Health", "Beauty",
                            "Toys", "Kids", "Baby", "Clothing", "Shoes", "Jewelery",
                            "Sports", "Outdoors", "Automotive", "Industrial"
                    ));
                    ps.setString(3, faker.lorem().sentence());
                    ps.setString(4, faker.commerce().productName());
                    ps.setInt(5, faker.number().numberBetween(1, 200));
                    ps.setString(6, faker.options().option("Piece", "Box", "Kg", "Pack", "Bottle"));
                    ps.setBigDecimal(7, new BigDecimal(faker.commerce().price(10.0, 2000.0)));
                    ps.addBatch();
                }

                ps.executeBatch();
                log.info("✅ Successfully inserted 1000 products!");
            }

        } catch (SQLException e) {
            log.error("❌ Error occurred during product seeding", e);
        }
    }
}
