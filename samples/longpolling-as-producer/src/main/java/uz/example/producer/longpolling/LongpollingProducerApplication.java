package uz.example.producer.longpolling;

import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uz.osoncode.easygram.core.provider.EasygramOkHttpClientProvider;

import java.time.Duration;

/**
 * Sample long-polling-as-producer bot.
 *
 * <p>All infrastructure (OkHttpClient, ObjectMapper, TelegramUrl, ExecutorService,
 * TelegramClient) falls back to the framework defaults. Override individual provider beans
 * if customisation is needed — see {@code longpolling-bot} for examples.</p>
 */
@SpringBootApplication
public class LongpollingProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LongpollingProducerApplication.class, args);
    }

    @Bean
    public EasygramOkHttpClientProvider okHttpClientProvider(){
        return () -> new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMinutes(1))
                .readTimeout(Duration.ofMinutes(1))
                .writeTimeout(Duration.ofMinutes(1))
                .build();
    }
}
