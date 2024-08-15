package com.odedia.translator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/srt")
public class TranslationService {
	private final Logger logger = LoggerFactory.getLogger(TranslationService.class);

	private final ChatClient chatClient;
    private int totalChunks = 0; // Total number of chunks to be processed
    private int processedChunks = 0; // Number of chunks processed so far

	public TranslationService(VectorStore vectorStore, 
								 ChatClient.Builder chatClientBuilder,
								 FileService fileService) throws IOException {
		this.chatClient = chatClientBuilder.build();
	}

    @PostMapping("translate")
    public ResponseEntity<String> translate(
            @RequestParam("sourceFile") MultipartFile sourceFile,
            @RequestParam String sourceLanguage,
            @RequestParam String targetLanguage) throws IOException {

        // Convert MultipartFile to String
        String srtContents = IOUtils.toString(sourceFile.getInputStream(), StandardCharsets.UTF_8);

        // Split by empty lines between subtitles
        List<String> chunks = Arrays.asList(srtContents.split("(?m)^\\s*$"));
        totalChunks = chunks.size(); // Update total chunks

        logger.info("Number of entries in SRT files is {}", totalChunks);

        StringBuilder sb = new StringBuilder();
        StringBuilder translatedStringBuilder = new StringBuilder();

        // Chunk by 50 subtitles every time to avoid token windows
        for (int i = 0; i < chunks.size(); i++) {
            sb.append(chunks.get(i));

            if (i % 50 == 0 && i > 0) {
                translateAndWrite(sb, sourceLanguage, targetLanguage, translatedStringBuilder);
                sb.setLength(0);
                processedChunks = i;
            }
        }
        translateAndWrite(sb, sourceLanguage, targetLanguage, translatedStringBuilder);
        processedChunks = chunks.size() + 1; // Update for the last chunk

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translated_file.srt")
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(translatedStringBuilder.toString());
    }

	private void translateAndWrite(StringBuilder sb, String sourceLanguage, String targetLanguage, StringBuilder translatedStringBuilder) throws IOException {
		System.out.println("""
				           About to translate:
				           ---
				           """);
		System.out.println(sb.toString());
		System.out.println("---");
		String translatedSrtChunk = chatClient.prompt()
				.system("""
						You are a subtitle translator. You support translation of SRT files only. For the input srt contents,
						provide an output translated to the desired target language. Do not provide any additional messages or friendly responses,
						you should only provide the translated srt in the response. remove any ``` markdown comments that you might consider including in the output.
						  """)
				.user(u -> u.text("""
						Translate the SRT contents below from {source} to {target}
						
						{srtContents}
 					    """).param("srtContents", sb.toString())
 					    		   .param("source", sourceLanguage)
 					    		   .param("target", targetLanguage)).call().content();

		System.out.println("""
		           Translated as follows:
		           ===
		           """);
		System.out.println(translatedSrtChunk);
		System.out.println("""
				   ===
				   """);
		
		translatedStringBuilder.append(translatedSrtChunk);
	}
	
    @GetMapping("progress")
    public ResponseEntity<Progress> getProgress() {
        Progress progress = new Progress(totalChunks, processedChunks);
        return ResponseEntity.ok(progress);
    }
    
    public static class Progress {
        private int totalChunks;
        private int processedChunks;

        public Progress(int totalChunks, int processedChunks) {
            this.totalChunks = totalChunks;
            this.processedChunks = processedChunks;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public int getProcessedChunks() {
            return processedChunks;
        }
    }
}
