package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Ну введите файл хотя бы...");
            return;
        }

        String filePath = args[0];
        String json = "";
        try {
            json = Files.readString(Paths.get(filePath));
        } catch (Exception e) {
            System.out.println("Файл не прочитался: " + e);
            return;
        }

        // Ну типо парсим JSON
        List<Map<String, Object>> всеБилеты = parseJsonTickets(json);
        List<Map<String, Object>> нужныеБилеты = new ArrayList<>();
        for (Map<String, Object> билет : всеБилеты) {
            if ("Владивосток".equals(билет.get("origin_name")) && "Тель-Авив".equals(билет.get("destination_name"))) {
                нужныеБилеты.add(билет);
            }
        }

        Map<String, Integer> минВремя = new HashMap<>();
        for (Map<String, Object> билет : нужныеБилеты) {
            String перевозчик = (String) билет.get("carrier");
            String вылет = (String) билет.get("departure_time");
            String прилет = (String) билет.get("arrival_time");
            int длительность = времяВМинутах(прилет) - времяВМинутах(вылет);
            if (длительность < 0) длительность += 24 * 60;

            if (!минВремя.containsKey(перевозчик) || длительность < минВремя.get(перевозчик)) {
                минВремя.put(перевозчик, длительность);
            }
        }


        System.out.println("Минимальное время полета для каждого перевозчика:");
        for (String перевозчик : минВремя.keySet()) {
            int m = минВремя.get(перевозчик);
            System.out.println(перевозчик + ": " + (m / 60) + "ч " + (m % 60) + "м");
        }
        
        List<Integer> цены = new ArrayList<>();
        for (Map<String, Object> билет : нужныеБилеты) {
            цены.add((Integer) билет.get("price"));
        }
        Collections.sort(цены);
        double средняя = 0;
        for (int p : цены) средняя += p;
        средняя /= цены.size();

        double медиана;
        int n = цены.size();
        if (n % 2 == 1) {
            медиана = цены.get(n / 2);
        } else {
            медиана = (цены.get(n / 2 - 1) + цены.get(n / 2)) / 2.0;
        }

        System.out.printf("\nРазница между средней ценой и медианой: %.2f\n", средняя - медиана);
    }

    static int времяВМинутах(String билет) {
        String[] p = билет.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    // парсер слепил, чтобы не тащить зависимость; может не работать на других форматах данных и на вложенностях
    static List<Map<String, Object>> parseJsonTickets(String content) {
        List<Map<String, Object>> list = new ArrayList<>();
        int start = content.indexOf("\"tickets\": [") + 11;
        int end = content.indexOf("]", start);
        String arr = content.substring(start, end);

        Pattern p = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
        Matcher m = p.matcher(arr);
        while (m.find()) {
            String obj = m.group(1);
            Map<String, Object> ticket = new HashMap<>();
            Pattern kv = Pattern.compile("\"(.*?)\"\\s*:\\s*(\".*?\"|\\d+)");
            Matcher mkv = kv.matcher(obj);
            while (mkv.find()) {
                String k = mkv.group(1);
                String v = mkv.group(2);
                ticket.put(k, v.startsWith("\"") ? v.substring(1, v.length() - 1) : Integer.valueOf(v));
            }
            list.add(ticket);
        }
        return list;
    }
}