package com.basterikus.SearchEngine.service.impl;

import com.basterikus.SearchEngine.config.IndexConfig;
import com.basterikus.SearchEngine.model.Site;
import com.basterikus.SearchEngine.model.Status;
import com.basterikus.SearchEngine.parser.IndexParser;
import com.basterikus.SearchEngine.parser.LemmaParser;
import com.basterikus.SearchEngine.parser.SiteIndex;
import com.basterikus.SearchEngine.repository.IndexRepository;
import com.basterikus.SearchEngine.repository.LemmaRepository;
import com.basterikus.SearchEngine.repository.PageRepository;
import com.basterikus.SearchEngine.repository.SiteRepository;
import com.basterikus.SearchEngine.service.IndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final IndexConfig indexConfig;
    private final SiteRepository siteRepository;
    private static final int processorCoreCount = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executorService = Executors.newFixedThreadPool(processorCoreCount);
    private final PageRepository pageRepository;
    private final LemmaParser lemmaParser;
    private final LemmaRepository lemmaRepository;
    private final IndexParser indexParser;
    private final IndexRepository indexRepository;

    @Override
    public void indexUrl(String url) {
        if (urlCheck(url)) {
            executorService.submit(new SiteIndex(pageRepository,
                    lemmaParser,
                    lemmaRepository,
                    indexParser,
                    indexRepository,
                    siteRepository,
                    url,
                    indexConfig));
            executorService.shutdown();
        } else {
            System.out.println("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
    }

    @Override
    public void indexAll() {
        if (isIndexingActive()) {
            System.out.println("Индексация уже запущена");
        } else {
            var urlList = indexConfig.getSite();
            for (Map<String, String> map : urlList) {
                String url = map.get("url");
                executorService.submit(new SiteIndex(pageRepository,
                        lemmaParser,
                        lemmaRepository,
                        indexParser,
                        indexRepository,
                        siteRepository,
                        url,
                        indexConfig));
            }
            executorService.shutdown();
        }
    }

    @Override
    public void stopIndexing() {
        if (isIndexingActive()) {
            executorService.shutdownNow();
            var siteList = siteRepository.findAll();
            for (Site site : siteList) {
                if (site.getStatus() == Status.INDEXING) {
                    site.setStatus(Status.FAILED);
                    siteRepository.save(site);
                }
            }
        } else {
            System.out.println("Индексация не запущена");
        }
    }

    private boolean isIndexingActive() {
        var siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private boolean urlCheck(String url) {
        var urlList = indexConfig.getSite();
        for (Map<String, String> map : urlList) {
            if (map.get("url").equals(url)) {
                return true;
            }
        }
        return false;
    }
}