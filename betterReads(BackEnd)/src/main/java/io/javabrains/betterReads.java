package io.javabrains;

import io.javabrains.Books.Book;
import io.javabrains.Books.bookRepository;
import io.javabrains.author.Author;
import io.javabrains.author.AuthorRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class betterReads {
    Logger logger = Logger.getLogger(getClass().getName());

    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private bookRepository bookRepository;

    // accessing the value that is the data
    @Value("${datadump.location.author}")
    private String authorDumpLocation;
    @Value("${datadump.location.works}")
    private String worksDumpLocation;


    public static void main(String[] args) {
        SpringApplication.run(betterReads.class, args);


    }

    @Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return builder -> builder.withCloudSecureConnectBundle(bundle);
    }


    private void initAuthors() {
        //get the path
        Path path = Paths.get(authorDumpLocation);
        // read them by lines
        try (Stream<String> lines = Files.lines(path)) {
            //limit the stream to stop when it gets the 10 elements
            lines.forEach(

                    line -> {
                        // read and parse the line
                        String jsonString = line.substring(line.indexOf("{"));


                        try {
                            JSONObject jsonObject = new JSONObject(jsonString);
                            //construct Author object
                            Author author = new Author();
                            author.setName(jsonObject.optString("name", "unknown"));
                            author.setId(jsonObject.optString("key").replace("/authors/", ""));
                            author.setPersonalName(jsonObject.optString("personal_name"));

                            // save in the Repository
                            logger.info("saving author Names: " + author.getName() + ".....");
                            authorRepository.save(author);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
            );


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initWorks() {
        //get the path
        Path path = Paths.get(worksDumpLocation);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        // read them by lines
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(

                    line -> {
                      //  logger.info("it works");
                        // read and parse the line
                        String jsonString = line.substring(line.indexOf("{"));
                        try {
                            JSONObject jsonObject = new JSONObject(jsonString);
                            // construct book objects
                            Book books = new Book();
                            books.setId(jsonObject.getString("key").replace("/works/", ""));
                            books.setName(jsonObject.optString("title"));

                            JSONObject descriptionObject = jsonObject.optJSONObject("description");

                            if (descriptionObject != null) {
                                books.setDescription(descriptionObject.optString("value"));

                            }
                            JSONObject publishObject = jsonObject.optJSONObject("created");
                            if (publishObject != null) {
                                String dataStr = publishObject.optString("value");
                                books.setPublishDate(LocalDate.parse(dataStr,dateTimeFormatter));
                            }
                            // getting the covers
                            JSONArray coversJSONArr = jsonObject.optJSONArray("covers"); // optJSONArray gets the json array
                            if (coversJSONArr != null) {
                                List<String> coverIds = new ArrayList<>();
                                for (int i = 0; i < coversJSONArr.length(); i++) {
                                    coverIds.add(coversJSONArr.getString(i));
                                }

                                books.setCoverIds(coverIds);
                            }

                            // getting the authorIds
                            JSONArray authorsObjectArr = jsonObject.optJSONArray("authors");
                            if (authorsObjectArr != null) {
                                List<String> authorIds = new ArrayList<>();
                                for (int i = 0; i < authorsObjectArr.length(); i++) {
                                    String authorId = authorsObjectArr.getJSONObject(i).getJSONObject("author").getString("key").replace("/authors/", "");
                                    authorIds.add(authorId);
                                }
                                books.setAuthorIds(authorIds);
                                List<String> authorNames = authorIds.stream().map(id -> authorRepository.findById(id))
                                        .map(optionalAuthor -> {
                                            if (!optionalAuthor.isPresent()) return " Unknown Author";
                                            return optionalAuthor.get().getName();
                                        }
                                ).collect(Collectors.toList());

                                books.setAuthorNames(authorNames);
                                logger.info("saving books: " + books.getName() + "...");


                            }
                            bookRepository.save(books);


                        } catch (Exception e) {

                            e.printStackTrace();

                        }
                    }

            );
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @PostConstruct
    public void start() {
         //initAuthors();
        initWorks();

    }


}
