package org.servantscode.commons.rest;

import java.util.List;

public class PaginatedResponse<T> {
    private int start;
    private int count;
    private int totalResults;
    private List<T> results;

    public PaginatedResponse() {}

    public PaginatedResponse(int start, int count, int totalResults, List<T> results) {
        this.start = start;
        this.count = count;
        this.totalResults = totalResults;
        this.results = results;
    }

    // ----- Accesssors -----
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public List<T> getResults() { return results; }
    public void setResults(List<T> results) { this.results = results; }
}
