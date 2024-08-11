package searchengine.dto.search;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Page;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.PageRepository;

import java.util.*;

@Log4j2
@UtilityClass
public class Relevance {

    public static Map<Page, Float> getRelativeRelevance(List<Page> pages
            , PageRepository pageRepository, int siteId, int limit, IndexesRepository indexesRepository) {
        Map<Page, Float> absoluteRelevanceMap = getAbsoluteRelevance(pages
                , pageRepository, siteId, indexesRepository);
        Optional<Float> max = absoluteRelevanceMap.values().stream().
                max((Float::compareTo));
        if (max.isPresent()) {
            Map<Page, Float> relativeRelevanceMap =
                    new TreeMap<>(Comparator.comparing(absoluteRelevanceMap::get));
            absoluteRelevanceMap.keySet().forEach(f -> {
                if ((limit > 0 && limit <= 50)
                        || absoluteRelevanceMap.size() < 200)
                    relativeRelevanceMap.put(f, absoluteRelevanceMap.get(f) / max.get());
            });
            return relativeRelevanceMap;
        } else return new HashMap<>();
    }

    public static Map<Page, Float> getAbsoluteRelevance(List<Page> pages
            , PageRepository pageRepository, int siteId, IndexesRepository indexesRepository) {
        Map<Page, Float> absoluteRelevanceMap = new HashMap<>();
        int pagesNumber = pageRepository.findCountPagesBySiteId(siteId);
        pages.forEach(p -> {

            List<Float> ranks = indexesRepository.findIndexByPageId(p.getId(), pagesNumber);
            Optional<Float> optRanksSum = ranks.stream().reduce(Float::sum);
            absoluteRelevanceMap.put(p, optRanksSum.orElse(0.0f));
        });

        Map<Page, Float> finalAbsoluteRelevanceMap = new TreeMap<>(Comparator.comparing(absoluteRelevanceMap::get));
        finalAbsoluteRelevanceMap.putAll(absoluteRelevanceMap);
        return finalAbsoluteRelevanceMap;
    }
}
