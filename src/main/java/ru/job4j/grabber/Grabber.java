package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Parse parse;
    private final Store store;
    private final Scheduler scheduler;
    private final int time;

    public Grabber(Parse parse, Store store, Scheduler scheduler, int time) {
        this.parse = parse;
        this.store = store;
        this.scheduler = scheduler;
        this.time = time;
    }

    @Override
    public void init() throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(time)
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public void web(Store store, Properties config) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(config.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        out.write(("<!doctype html><html><head><meta charset=\"windows-1251\" />"
                                + "<title>Page with vacancies</title></head><body>").getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.toString().getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                        }
                        out.write("</body></html>".getBytes());
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static class GrabJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            List<Post> vacanciesList = parse.list("https://career.habr.com/vacancies/java_developer");
            for (Post onePost : vacanciesList) {
                store.save(onePost);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try (InputStream input = Grabber.class.getClassLoader()
                .getResourceAsStream("db/rabbit.properties")) {
            config.load(input);
        }
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        var parse = new HabrCareerParse(new HabrCareerDateTimeParser());
        var store = new PsqlStore(config);
        var time = Integer.parseInt(config.getProperty("rabbit.interval"));
        Grabber grab = new Grabber(parse, store, scheduler, time);
        grab.init();
        grab.web(store, config);
    }
}