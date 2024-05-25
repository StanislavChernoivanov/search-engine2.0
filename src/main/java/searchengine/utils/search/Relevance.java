package searchengine.utils.search;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import searchengine.model.entities.Page;
import searchengine.model.repositories.PageRepository;
import java.util.*;

@RequiredArgsConstructor
@Log4j2
public class Relevance {

    public static Map<Page, Float> getRelativeRelevance(List<Page> pages
            , PageRepository pageRepository, int siteId, int limit) {
        Map<Page, Float> absoluteRelevanceMap = getAbsoluteRelevance(pages
                , pageRepository, siteId);
        Optional<Float> max = absoluteRelevanceMap.values().stream().
                max((Float::compareTo));
        if (max.isPresent()) {
            Map<Page, Float> relativeRelevanceMap =
                    new TreeMap<>(Comparator.comparing(absoluteRelevanceMap::get));
            absoluteRelevanceMap.keySet().forEach(f -> {
                if((limit > 0 && limit <= 50)
                        || absoluteRelevanceMap.size() < 200)
                    relativeRelevanceMap.put(f, absoluteRelevanceMap.get(f) / max.get());
            });
            return relativeRelevanceMap;
        } else return new HashMap<>();
    }

    public static Map<Page, Float> getAbsoluteRelevance(List<Page> pages
            , PageRepository pageRepository, int siteId) {
        Map<Page, Float> absoluteRelevanceMap = new HashMap<>();
        int pagesNumber = pageRepository.findCountPagesBySiteId(siteId);
        pages.forEach(p -> {
            Optional<Float> sum = p.getIndexList().stream().map(index -> {
                float commonPercent = (float) index.getLemma().getFrequency() /
                        pagesNumber * 100;
                if (commonPercent < 30) return index.getRank();
                else return 0.0f;
            }).reduce(Float::sum);
            sum.ifPresent(aFloat -> absoluteRelevanceMap.put(p, aFloat));
        });

        Map<Page, Float> finalAbsoluteRelevanceMap = new TreeMap<>(Comparator.comparing(absoluteRelevanceMap::get));
        finalAbsoluteRelevanceMap.putAll(absoluteRelevanceMap);
        return finalAbsoluteRelevanceMap;
    }
}
