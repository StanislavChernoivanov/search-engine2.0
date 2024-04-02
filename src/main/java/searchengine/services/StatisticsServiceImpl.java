package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.utils.Response;
import searchengine.utils.statistics.DetailedStatisticsItem;
import searchengine.utils.statistics.StatisticsData;
import searchengine.utils.statistics.StatisticsResponse;
import searchengine.utils.statistics.TotalStatistics;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public Response getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(siteRepository.getCountSites());
        total.setPages(pageRepository.getCountPages());
        total.setLemmas(lemmaRepository.getCountLemmas());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<searchengine.model.entities.Site> sites = siteRepository.findAll();
        if(sites.isEmpty())
            return new Response(false, "Сайты не доступны. Убедитесь, что индексация проведена");
        sites.forEach(s -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(s.getName());
            item.setUrl(s.getUrl());
            item.setPages(pageRepository.findCountPagesBySiteId(s.getId()));
            item.setLemmas(lemmaRepository.findCountLemmasBySiteId(s.getId()));
            item.setStatus(String.valueOf(s.getStatus()));
            item.setError(s.getLastError());
            item.setStatusTime(s.getStatusTime());
            detailed.add(item);
        });
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        return new StatisticsResponse(true, "", data);
    }
}
