package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.FailResponse;
import searchengine.dto.Response;
import searchengine.model.entities.Indexes;
import searchengine.model.entities.Lemma;
import searchengine.model.entities.Page;
import searchengine.model.entities.Site;
import searchengine.model.repositories.IndexesRepository;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.repositories.SiteRepository;
import searchengine.services.IndexPageService;
import searchengine.utils.startIndexing.LemmaCreator;
import searchengine.utils.startIndexing.LemmaHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexPageServiceImpl implements IndexPageService {

    private final LemmaRepository lemmaRepository;
    private final IndexesRepository indexesRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private Document doc;
    private Connection connection;
    private final SitesList sites;
    private final LemmaHandler lemmaHandler;


    @Override
    public Response indexPage(String pageUrl) throws InterruptedException {
        URL url;
        try {
            url = new URI(pageUrl).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            return new FailResponse(false, "Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        } catch (IllegalArgumentException ex) {
            return new FailResponse(false, "Введите абсолютный URI");
        }
        Response response = new Response(true);
        Optional<Page> optionalPage = Optional.ofNullable(pageRepository.findPageByPath(url.getPath()));
        if (optionalPage.isPresent()) {
            return updatePage(optionalPage.get(), url, response);
        } else {
            return addNewPage(url, response);
        }
    }

    private Response addNewPage(URL url, Response response) throws InterruptedException {
        List<searchengine.config.Site> siteList = sites.getSites();
        Optional<searchengine.config.Site> isSiteExist = siteList.stream().filter(s ->
                s.getUrl().contains(url.getHost())).findFirst();
        if (isSiteExist.isEmpty())
            return new FailResponse(false, "Данная страница находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
        else {
            Optional<Site> optionalSite = siteRepository.findSiteByUrl(isSiteExist.get().getUrl());
            Site siteModel;
            String urlSite = isSiteExist.get().getUrl();
            if (optionalSite.isEmpty() || !optionalSite.get().getUrl().equals(urlSite)) {
                siteModel = new Site();
                siteModel.setUrl(isSiteExist.get().getUrl());
                siteModel.setName(isSiteExist.get().getName());
                siteRepository.save(siteModel);
            } else siteModel = optionalSite.get();
            if (indexPageIFPageIsAvailable(url, siteModel)) {
                return new FailResponse(false, "Отсутствует соединение с данной страницей");
            }
            return response;
        }
    }

    private Response updatePage(Page outdatedPage, URL url, Response response) throws InterruptedException {
        Site site = outdatedPage.getSite();
        List<Indexes> indexList = indexesRepository.
                findIndexWithSpecifiedPageId(outdatedPage.getId());
        List<Optional<Lemma>> optLemmaList = new ArrayList<>();
        indexList.forEach(i -> optLemmaList.add(Optional.ofNullable(i.getLemma())));
        indexesRepository.deleteAll(indexList);
        handleLemma(optLemmaList);
        optLemmaList.clear();
        pageRepository.delete(outdatedPage);
        indexList.clear();
        if (indexPageIFPageIsAvailable(url, site))
            return new FailResponse(false, "Отсутствует соединение с данной страницей");
        return response;
    }

    private void handleLemma(List<Optional<Lemma>> optLemmaList) {
        List<Lemma> lemmasForDeleteFromDB = new ArrayList<>();
        List<Lemma> lemmasForEditing = new ArrayList<>();
        optLemmaList.forEach(l -> {
            if (l.isPresent()) {
                Lemma lemma = l.get();
                if (lemma.getFrequency() == 1) {
                    lemmasForDeleteFromDB.add(lemma);
                } else {
                    lemma.setFrequency((lemma.getFrequency() - 1));
                    lemmasForEditing.add(lemma);
                }
            }
        });
        lemmaRepository.deleteAll(lemmasForDeleteFromDB);
        lemmaRepository.saveAll(lemmasForEditing);
    }

    private boolean indexPageIFPageIsAvailable(URL url, Site site) throws InterruptedException {
        if (isAvailableWebPage(url)) {
            Page newPage = new Page();
            newPage.setPath(url.getPath());
            newPage.setCode(connection.response().statusCode());
            newPage.setContent(doc.html());
            newPage.setSite(site);
            pageRepository.save(newPage);
            LemmaCreator lemmaCreator =
                    new LemmaCreator(site, lemmaHandler, List.of(newPage));
            Thread thread = new Thread(lemmaCreator);
            thread.start();
            thread.join();
            lemmaHandler.saveRemainedLemmasInDB();
            return false;
        } else {
            Page newPage = new Page();
            newPage.setPath(url.getPath());
            newPage.setCode(404);
            newPage.setContent("");
            newPage.setSite(site);
            pageRepository.save(newPage);
            return true;
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