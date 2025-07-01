package com.ht.eventbox.config;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfiguration {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dwbwvdguk",
                "api_key", "422919675926511",
                "api_secret", "hyuteQFyNtndoXnOP6FwTDqgoos"));
    }
}
