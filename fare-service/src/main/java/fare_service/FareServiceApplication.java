package fare_service;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(scanBasePackages = {"com.ridelink.fare", "fare_service"})
@EnableMongoRepositories(basePackages = {"com.ridelink.fare", "fare_service"})
public class FareServiceApplication {

	private static MongoServer inMemoryMongoServer;

	static {
		loadEnv();
	}

	public static void main(String[] args) {
		SpringApplication.run(FareServiceApplication.class, args);
	}

	private static void loadEnv() {
		File envFile = new File(".env");
		if (!envFile.exists()) {
			envFile = new File("fare-service/.env");
		}
		if (!envFile.exists()) {
			envFile = new File("../fare-service/.env");
		}

		if (envFile.exists()) {
			try (FileInputStream fis = new FileInputStream(envFile)) {
				Properties props = new Properties();
				props.load(fis);
				props.forEach((k, v) -> System.setProperty(k.toString(), v.toString()));
				System.out.println(">>> Successfully loaded .env from: " + envFile.getAbsolutePath());
			} catch (Exception e) {
				System.err.println(">>> Could not load .env file: " + e.getMessage());
			}
		} else {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		}

		configureDatabase();
	}

	private static void configureDatabase() {
		String mongoUri = System.getProperty("MONGODB_URI");
		if (mongoUri == null || mongoUri.isBlank()) {
			mongoUri = System.getProperty("MONGO_URI");
		}
		boolean useInMemory = "true".equalsIgnoreCase(System.getProperty("USE_IN_MEMORY_MONGO", "false"));

		if (!useInMemory && mongoUri != null && !mongoUri.isBlank()) {
			System.out.println(">>> Verifying connection to MongoDB Atlas for fare-service...");
			try (MongoClient testClient = MongoClients.create(
					MongoClientSettings.builder()
							.applyConnectionString(new ConnectionString(mongoUri))
							.applyToSocketSettings(b -> b.connectTimeout(3, TimeUnit.SECONDS))
							.applyToClusterSettings(b -> b.serverSelectionTimeout(3, TimeUnit.SECONDS))
							.build())) {
				testClient.getDatabase("admin").runCommand(new Document("ping", 1));
				System.out.println(">>> Connected successfully to MongoDB Atlas: " + mongoUri.replaceAll(":[^@]+@", ":****@"));
				System.setProperty("spring.mongodb.uri", mongoUri);
				System.setProperty("spring.data.mongodb.uri", mongoUri);
				return;
			} catch (Exception e) {
				System.err.println(">>> ⚠️ Could not connect to MongoDB Atlas (" + e.getMessage() + ")");
				System.err.println(">>> ⚠️ Starting embedded in-memory MongoDB instance for local testing & development...");
			}
		}

		try {
			inMemoryMongoServer = new MongoServer(new MemoryBackend());
			InetSocketAddress serverAddress = inMemoryMongoServer.bind();
			String inMemoryUri = "mongodb://localhost:" + serverAddress.getPort() + "/fare_db";
			System.out.println(">>> [INFO] Started In-Memory MongoDB Server on: " + inMemoryUri);
			System.setProperty("spring.mongodb.uri", inMemoryUri);
			System.setProperty("spring.data.mongodb.uri", inMemoryUri);
		} catch (Exception e) {
			System.err.println(">>> [ERROR] Failed to start in-memory MongoDB server: " + e.getMessage());
		}
	}
}
