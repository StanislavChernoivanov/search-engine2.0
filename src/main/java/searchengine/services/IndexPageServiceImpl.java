package searchengine.services;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.utils.startIndexing.LemmaIndexer;
import searchengine.utils.Response;
import searchengine.model.entities.*;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.utils.startIndexing.SaverOrRefresher;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Service
@Log4j2
public class IndexPageServiceImpl  implements IndexPageService{

        private final LemmaRepository lemmaRepository;
        private final IndexesRepository indexesRepository;
        private final PageRepository pageRepository;
        private final SiteRepository siteRepository;
        private Document doc;
        private Connection connection;
        private final SitesList sites;
        private final SaverOrRefresher saverOrRefresher;

    public IndexPageServiceImpl(LemmaRepository lemmaRepository,
                                IndexesRepository indexesRepository,
                                PageRepository pageRepository,
                                SiteRepository siteRepository,
                                SitesList sites) {
        this.lemmaRepository = lemmaRepository;
        this.indexesRepository = indexesRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.sites = sites;
        saverOrRefresher = new SaverOrRefresher(lemmaRepository);
    }




    @Override
    public Response indexPage(URL url) throws InterruptedException {
        Response response = new Response(true, "");
        Optional<Page> optionalPage = Optional.ofNullable(pageRepository.findPageByPath(url.getPath()));
        if (optionalPage.isPresent()) {
            Page outdatedPage = pageRepository.findPageByPath(url.getPath());
            Site site = outdatedPage.getSite();
            List<Indexes> indexList = indexesRepository.
                    findIndexWithSpecifiedPageId(outdatedPage.getId());
            List<Optional<Lemma>> lemmaList = new ArrayList<>();
            indexList.forEach(i -> lemmaList.add(Optional.ofNullable(i.getLemma())));
            indexList.forEach(indexesRepository::delete);
            lemmaList.forEach(l -> {
                if (l.isPresent()) {
                    Lemma lemma = l.get();
                    if (lemma.getFrequency() == 1) {
                        lemmaRepository.delete(lemma);
                    } else {
                        lemma.setFrequency((lemma.getFrequency() - 1));
                        lemmaRepository.save(lemma);
                    }
                }
            });
            lemmaList.clear();
            pageRepository.delete(outdatedPage);
            indexList.clear();
            if (isAvailableWebPage(url)) {
                if(!indexPageIfPageExist(url))
                    return new Response(false, "Отсутствует соединение с данной страницей");
            }
        } else {
            List<searchengine.config.Site> siteList = sites.getSites();
            if (siteList.stream().noneMatch(s -> s.getUrl().contains(url.getHost())))
                return new Response(false, "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле");
            else {
                searchengine.config.Site site = siteList.stream().filter(s ->
                        s.getUrl().contains(url.getHost())).findFirst().get();
                Optional<Site> optionalSite = siteRepository.findSiteByUrl(site.getUrl());
                Site siteModel;
                String urlSite = site.getUrl();
                if (optionalSite.isEmpty() || !optionalSite.get().getUrl().equals(urlSite)) {
                    siteModel = new Site();
                    siteModel.setUrl(site.getUrl());
                    siteModel.setName(site.getName());
                    siteRepository.save(siteModel);
                }
                else siteModel = optionalSite.get();
                if (!isAvailableWebPage(url)) {
                    Page newPage = new Page();
                    newPage.setPath(url.getPath());
                    newPage.setCode(404);
                    newPage.setContent("");
                    newPage.setSite(siteModel);
                    pageRepository.save(newPage);
                    return new Response(false, "Отсутствует соединение с данной страницей");
                } else {
                    Page newPage = new Page();
                    newPage.setPath(url.getPath());
                    newPage.setCode(connection.response().statusCode());
                    newPage.setContent(doc.html());
                    newPage.setSite(siteModel);
                    pageRepository.save(newPage);

                    LemmaIndexer lemmaIndexer =
                            new LemmaIndexer(newPage, saverOrRefresher);
                    Thread thread = new Thread(lemmaIndexer);
                    thread.start();
                    thread.join();
                }
            }
        }
        return response;
    }

    private boolean indexPageIfPageExist(URL url) throws InterruptedException {
        Page outdatedPage = pageRepository.findPageByPath(url.getPath());
        Site site = outdatedPage.getSite();
        List<Indexes> indexList = indexesRepository.
                findIndexWithSpecifiedPageId(outdatedPage.getId());
        List<Optional<Lemma>> lemmaList = new ArrayList<>();
        indexList.forEach(i -> lemmaList.add(Optional.ofNullable(i.getLemma())));
        indexList.forEach(indexesRepository::delete);
        lemmaList.forEach(l -> {
            if (l.isPresent()) {
                Lemma lemma = l.get();
                if (lemma.getFrequency() == 1) {
                    lemmaRepository.delete(lemma);
                } else {
                    lemma.setFrequency((lemma.getFrequency() - 1));
                    lemmaRepository.save(lemma);
                }
            }
        });
        lemmaList.clear();
        pageRepository.delete(outdatedPage);
        indexList.clear();
        if (isAvailableWebPage(url)) {
            Page newPage = new Page();
            newPage.setPath(url.getPath());
            newPage.setCode(connection.response().statusCode());
            newPage.setContent(doc.html());
            newPage.setSite(outdatedPage.getSite());
            pageRepository.save(newPage);
            LemmaIndexer lemmaIndexer =
                    new LemmaIndexer(newPage, saverOrRefresher);
            Thread thread = new Thread(lemmaIndexer);
            thread.start();
            thread.join();
            saverOrRefresher.saveRemainedLemmasInDB();
            return true;
        } else {
            Page newPage = new Page();
            newPage.setPath(url.getPath());
            newPage.setCode(404);
            newPage.setContent("");
            newPage.setSite(site);
            pageRepository.save(newPage);
            return false;
        }
    }

    private boolean isAvailableWebPage(URL url) {
        connection = Jsoup.connect(url.toString());
        try {
            doc = connection.
                    userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                            "Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com").maxBodySize(0).get();
        } catch (IOException e) {
            log.error("{}\n{}", e.getMessage(), e.getStackTrace());
            return false;
        }
        return true;
    }
}