package com.demo.ai.app.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/image")
public class ImageGenerationController {
    private final ChatClient chatClient;
    private final OpenAiImageModel imageModel;

    public ImageGenerationController(OpenAiImageModel imageModel, ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.imageModel = imageModel;
    }

    @GetMapping("/generate-image/{prompt}")
    public String generateImage(@PathVariable String prompt) throws Exception {
        ImagePrompt imagePrompt = new ImagePrompt(prompt,
                OpenAiImageOptions.builder()
                        .quality("high")
                        .height(1024)
                        .width(1024)
                        .build());
        ImageResponse imageResponse = imageModel.call(imagePrompt);

//        String url=imageResponse.getResult().getOutput().getUrl();
//        System.out.println(url);
        String base64Json = imageResponse.getResult().getOutput().getB64Json();
        byte[] bytesImage = Base64.getDecoder().decode(base64Json);
        Files.write(Paths.get("ai-generated-image.png"), bytesImage);
        return "Image is generated successfully";
    }

    @GetMapping(value = "/generate-image-output/{prompt}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateImageOutput(@PathVariable String prompt) {
        try {
            ImagePrompt imagePrompt = new ImagePrompt(prompt,
                    OpenAiImageOptions.builder()
                            .quality("high")
                            .height(1024)
                            .width(1024)
                            .build());

            ImageResponse imageResponse = imageModel.call(imagePrompt);

            // Extract the base64 string from the response
            String base64Json = imageResponse.getResult().getOutput().getB64Json();

            // Decode the base64 string back into raw image bytes
            byte[] bytesImage = Base64.getDecoder().decode(base64Json);

            // Optional: If you still want to save a copy locally on your server
            // Files.write(Paths.get("ai-generated-image.png"), bytesImage);

            // Return the bytes directly with a 200 OK status and IMAGE_PNG content type
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytesImage);

        } catch (Exception e) {
            // Good practice to handle exceptions gracefully
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/describe-image")
    public String DescribeImage(@RequestParam String prompt,
                                @RequestParam MultipartFile file) {
        return chatClient.prompt()
                .user(us -> us.text(prompt)
                        .media(MimeTypeUtils.IMAGE_JPEG, file.getResource()))
                .call()
                .content();
    }
}
