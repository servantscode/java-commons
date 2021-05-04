package org.servantscode.commons.csv;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CSVData {
    public String headers = null;
    public int failedRows = 0;
    public int totalRows = 0;
    public LinkedList<String> fields = new LinkedList<>();
    public HashMap<String, AtomicInteger> fieldCounts = new HashMap<>(1024);
    public List<Map<String, String>> rowData = new LinkedList<>();
    public List<String> badLines = new LinkedList<>();
}
