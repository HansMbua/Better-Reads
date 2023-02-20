package io.javabrains.Search;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {
    private final WebClient webClient;// building it once and using it in my controller

    private final String COVER_IMAGE_ROOT = "http://covers.openlibrary.org/b/id/";

    public SearchController(WebClient.Builder webClientBuilder) {
        // do research its works like rest template
        // fix the search to not exceed a given max level
        this.webClient = webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build()).baseUrl("http://openlibrary.org/search.json").build();
    }

    @GetMapping(value = "/search")
    public String getSearchResults(@RequestParam String query, Model model){
        Mono<SearchResult> searchResultMono = this.webClient.get()
                .uri("?q={query}", query)
                .retrieve().bodyToMono(SearchResult.class);

        SearchResult searchResult = searchResultMono.block(); // block gets the result
        List<SearchResultBook> books = searchResult.getDocs()
                .stream()
                .limit(10)
                .map(bookResult ->{
                    bookResult.setKey(bookResult.getKey().replace("/works/",""));
                    String coverId = bookResult.getCover_i();
                    if(StringUtils.hasText(coverId)){
                        coverId = COVER_IMAGE_ROOT + coverId+"-M.jpg";
                    }else {
                        coverId = "/images/no_image.png";
                    }
                    bookResult.setCover_i(coverId);
                    return bookResult;
                })
                .collect(Collectors.toList());
        model.addAttribute("searchResult",books);

        return "Search";

    }
}
