package com.example.back_end.dto;

import java.util.ArrayList;
import java.util.List;

public class TeacherAnalyticsDtos {

    public static class DashboardResponse {
        public String name;
        public Hero hero = new Hero();
        public List<SummaryCard> summary = new ArrayList<>();
        public List<CourseCard> courses = new ArrayList<>();
        public InteractionBlock interactions = new InteractionBlock();
        public List<ReviewCard> reviews = new ArrayList<>();
        public List<TrendSeries> trends = new ArrayList<>();
    }

    public static class Hero {
        public int pending;
        public int resolved;
    }

    public static class SummaryCard {
        public String id;
        public String label;
        public String value;
        public String diff;
    }

    public static class CourseCard {
        public String title;
        public long enrolled;
        public int completion;
        public long drop;
        public double rating;
    }

    public static class InteractionBlock {
        public NoteStats notes = new NoteStats();
        public ResourceStats resources = new ResourceStats();
        public SupportStats support = new SupportStats();
        public List<NotePreview> latestNotes = new ArrayList<>();
    }

    public static class NoteStats {
        public int pending;
        public int awaiting;
        public int resolved;
    }

    public static class ResourceStats {
        public int pending;
        public int approved;
        public int hidden;
    }

    public static class SupportStats {
        public int open;
        public String avgResponse;
    }

    public static class NotePreview {
        public String course;
        public String student;
        public String content;
        public String at;
    }

    public static class ReviewCard {
        public String course;
        public double avg;
        public int recommend;
        public int newReviews;
    }

    public static class TrendSeries {
        public String id;
        public String label;
        public List<Integer> values = new ArrayList<>();
        public String color;
    }
}
