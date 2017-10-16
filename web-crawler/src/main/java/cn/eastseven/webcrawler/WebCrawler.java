package cn.eastseven.webcrawler;

import cn.eastseven.webcrawler.downloader.WebDriverDownloader;
import cn.eastseven.webcrawler.model.*;
import cn.eastseven.webcrawler.pipeline.ChinaPubPipeline;
import cn.eastseven.webcrawler.pipeline.MongoPipeline;
import cn.eastseven.webcrawler.pipeline.WinXuanPipeline;
import cn.eastseven.webcrawler.repository.ChinaPubRepository;
import cn.eastseven.webcrawler.repository.DangDangRepository;
import cn.eastseven.webcrawler.repository.WinXuanRepository;
import cn.eastseven.webcrawler.service.ProxyService;
import cn.eastseven.webcrawler.utils.SiteUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.scheduler.RedisScheduler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@Order(value = 2)
public class WebCrawler implements CommandLineRunner {

    @Autowired
    ApplicationContext context;

    @Autowired
    ProxyService proxyService;

    @Autowired
    ChinaPubPipeline chinaPubPipeline;

    @Autowired
    WinXuanPipeline winXuanPipeline;

    @Autowired
    RedisScheduler redisScheduler;

    @Autowired
    MongoPipeline mongoPipeline;

    @Autowired
    WebDriverDownloader downloader;

    @Autowired
    ChinaPubRepository chinaPubRepository;

    @Autowired
    DangDangRepository dangDangRepository;

    @Autowired
    WinXuanRepository winXuanRepository;

    @Autowired
    ExecutorService executorService;

    final Class[] pageModels = {
            ChinaPub.class,
            WinXuan.class,
            DangDang.class
    };

    public void start() {
        log.info(">>> start <<<");

        List<Spider> spiderList = Lists.newArrayList();
        for (Class pageModel : pageModels) {
            SeedUrl seedUrl = AnnotationUtils.findAnnotation(pageModel, SeedUrl.class);
            log.debug("seedUrl value= {}", Arrays.toString(seedUrl.value()));

            Request[] requests = new Request[seedUrl.value().length];
            for (int index = 0; index < requests.length; index++) {
                Request request = new Request(seedUrl.value()[index]);
                request.putExtra("ignore", Boolean.TRUE);
                requests[index] = request;
            }

            Spider spider = OOSpider.create(SiteUtil.get(), mongoPipeline, pageModel)
                    .setScheduler(redisScheduler)
                    .addRequest(requests)
                    .thread(executorService, requests.length);

            spiderList.add(spider);
        }

        for (Spider spider : spiderList) {
            spider.start();
        }

        log.info(">>> end <<<");
    }

    public void update() {
        update(BookOrigin.WIN_XUAN);
        update(BookOrigin.CHINA_PUB);
        update(BookOrigin.DANG_DANG);
    }

    public void update(BookOrigin origin) {
        final int size = 10;
        int totalPages;
        Spider spider = null;
        switch (origin) {
            case CHINA_PUB:
                spider = OOSpider.create(SiteUtil.get(), mongoPipeline, ChinaPub.class)
                        .setScheduler(redisScheduler).thread(executorService, 1);

                Page<ChinaPub> first = chinaPubRepository.findByCreateTimeIsNull(new PageRequest(0, size));
                if (!first.hasContent()) {
                    break;
                }

                totalPages = first.getTotalPages();
                log.info("\t>>>\tCHINA_PUB\ttotal={}, total pages={}", first.getTotalElements(), totalPages);

                for (int page = 0; page < totalPages; page++) {
                    PageRequest pageRequest = new PageRequest(page, size);
                    Page<ChinaPub> chinaPubPage = chinaPubRepository.findByCreateTimeIsNull(pageRequest);
                    for (ChinaPub chinaPub : chinaPubPage) {
                        Request request = new Request(chinaPub.getUrl());
                        request.putExtra("ignore", Boolean.TRUE);
                        spider.addRequest(request);
                    }

                    if (page > 1) {
                        break;
                    }
                }

                break;
            case DANG_DANG:
                spider = OOSpider.create(SiteUtil.get(), mongoPipeline, DangDang.class)
                        .setScheduler(redisScheduler).thread(executorService, 1);

                Page<DangDang> firstDangDangPage = dangDangRepository.findByCreateTimeIsNull(new PageRequest(0, size));
                if (!firstDangDangPage.hasContent()) {
                    break;
                }

                totalPages = firstDangDangPage.getTotalPages();
                log.info("\t>>>\tDANG_DANG\ttotal={}, total pages={}", firstDangDangPage.getTotalElements(), totalPages);

                for (int page = 0; page < totalPages; page++) {
                    PageRequest pageRequest = new PageRequest(page, size);
                    Page<DangDang> dangDangPage = dangDangRepository.findByCreateTimeIsNull(pageRequest);
                    for (DangDang dangDang : dangDangPage) {
                        Request request = new Request(dangDang.getUrl());
                        request.putExtra("ignore", Boolean.TRUE);
                        spider.addRequest(request);
                    }

                    if (page > 1) {
                        break;
                    }
                }

                break;
            case WIN_XUAN:
                spider = OOSpider.create(SiteUtil.get(), mongoPipeline, WinXuan.class)
                        .setScheduler(redisScheduler).thread(executorService, 1);

                Page<WinXuan> firstWinXuanPage = winXuanRepository.findByCreateTimeIsNull(new PageRequest(0, size));
                if (!firstWinXuanPage.hasContent()) {
                    break;
                }

                totalPages = firstWinXuanPage.getTotalPages();
                log.info("\t>>>\tWIN_XUAN\ttotal={}, total pages={}", firstWinXuanPage.getTotalElements(), totalPages);

                for (int page = 0; page < totalPages; page++) {
                    PageRequest pageRequest = new PageRequest(page, size);
                    Page<WinXuan> winXuanPage = winXuanRepository.findByCreateTimeIsNull(pageRequest);
                    for (WinXuan winXuan : winXuanPage) {
                        Request request = new Request(winXuan.getUrl());
                        request.putExtra("ignore", Boolean.TRUE);
                        spider.addRequest(request);
                    }

                    if (page > 1) {
                        break;
                    }
                }
                break;

            default:
                break;
        }

        if (spider == null) {
            return;
        }
        log.info("{} page count {}", origin, spider.getPageCount());
        spider.runAsync();
    }

    @Override
    public void run(String... strings) throws Exception {
        log.info(" === Entry {}", Arrays.toString(strings));
        for (String param : strings) {
            switch (param) {
                case "start":
                    start();
                    break;
                case "updateChinaPub":
                    update(BookOrigin.CHINA_PUB);
                    break;
                case "update":
                    update();
                    break;
                default:
                    break;
            }
        }
    }
}
