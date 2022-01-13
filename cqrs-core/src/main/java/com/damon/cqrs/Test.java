package com.damon.cqrs;


import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) {
        List<Event> list = new ArrayList<>();
        list.add(new Event(1,"a1"));
        list.add(new Event(1,"a2"));
        list.add(new Event(2,"a1"));

        list.add(new Event(3,"a1"));

        list.add(new Event(4,"a1"));


        list.add(new Event(2,"a2"));

        list.add(new Event(3,"a2"));

        list.add(new Event(4,"a2"));

        Map<String, List<Event>> events = list.stream().collect(Collectors.groupingBy(
                Event::getType
        ));
        System.out.println(events);
    }
    public static class Event{
        public Event(Integer version, String type) {
            this.version = version;
            this.type = type;
        }

        private Integer version;
        private String type;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "version=" + version +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
