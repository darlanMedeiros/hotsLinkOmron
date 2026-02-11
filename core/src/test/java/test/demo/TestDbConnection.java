package test.demo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.ctrl.db.config.DbConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestDbConnection {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DbConfig.class);
        try {
            javax.sql.DataSource ds = ctx.getBean(javax.sql.DataSource.class);
            try (Connection conn = ds.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("Connected: " + meta.getURL());
                System.out.println("User: " + meta.getUserName());
                System.out.println("Schema: " + conn.getSchema());
                System.out.println("DB Product: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ctx.close();
        }
    }
}
