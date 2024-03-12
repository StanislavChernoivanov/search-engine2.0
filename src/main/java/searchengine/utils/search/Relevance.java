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
            , PageRepository pageRepository, int siteId) {
        Map<Page, Float> absoluteRelevanceMap = getAbsoluteRelevance(pages
                , pageRepository, siteId);
        Optional<Float> max = absoluteRelevanceMap.values().stream().
                max((Float::compareTo));
        if (max.isPresent()) {
            Map<Page, Float> relativeRelevanceMap =
                    new TreeMap<>(Comparator.comparing(absoluteRelevanceMap::get));
            absoluteRelevanceMap.keySet().forEach(f -> relativeRelevanceMap
                    .put(f, absoluteRelevanceMap.get(f) / max.get()));
            return relativeRelevanceMap;
        } else throw new RuntimeException();
    }

    public static Map<Page, Float> getAbsoluteRelevance(List<Page> pages
            , PageRepository pageRepository, int siteId) {
        Map<Page, Float> absoluteRelevanceMap = new HashMap<>();
        pages.forEach(p -> {
            Optional<Float> sum = p.getIndexList().stream().
                    filter(i -> i.getPage().getPath().equals(p.getPath()))
                    .map(index -> {
                        float commonPercent = (float) index.getLemma().getFrequency() /
                                pageRepository.findCountPagesBySiteId(siteId) * 100;
                        if(commonPercent < 30) return index.getRank();
                        else return 0.0f;
                    }).reduce(Float::sum);
            sum.ifPresent(aFloat -> absoluteRelevanceMap.put(p, aFloat));
        });
        return absoluteRelevanceMap;
    }
}
