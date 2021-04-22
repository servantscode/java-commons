package org.servantscode.commons.csv;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class CSVParser {
    private int headerRow;
    private int firstDataRow;
    private boolean hasHeaders;

    public CSVParser() {
        this(0, 1);
    }

    public CSVParser(int headerRow, int firstDataRow) {
        this.headerRow = headerRow;
        this.firstDataRow = firstDataRow;
        hasHeaders = headerRow > -1;
    }

    public CSVData readFiles(List<File> importFiles) {
        CSVData data = new CSVData();
        importFiles.forEach(f -> doRead(data, f));
        return data;
    }

    public CSVData readFile(File importFile) {
        CSVData data = new CSVData();
        doRead(data, importFile);
        return data;
    }

    public CSVData readStream(InputStream fileBytes) {
        CSVData data = new CSVData();
        readStream(data, new BufferedReader(new InputStreamReader(fileBytes)));
        return data;
    }

    private void doRead(CSVData data, File importFile) {
        try {
            BufferedReader fileLines = new BufferedReader(new FileReader(importFile));
            readStream(data, fileLines);

            System.out.println(String.format("Processed %s -- %d lines. %d failures", importFile.getCanonicalPath(), data.totalRows, data.failedRows));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + importFile.getName());
        }

        if(!data.badLines.isEmpty()) {
            System.out.println(data.headers);
            data.badLines.forEach(System.err::println);
        }
    }

    private void readStream(CSVData data, BufferedReader fileLines) {
        int lineNumber = 0;
        try {
            while(++lineNumber < headerRow)
                fileLines.readLine();

            if(hasHeaders) {
                data.headers = fileLines.readLine();
                processHeaders(data, data.headers);
            }

            while(lineNumber < firstDataRow-1) {
                System.out.println("Skipping row");
                fileLines.readLine();
                lineNumber++;
            }


            String line = null;
            while((line = fileLines.readLine()) != null) {
                lineNumber++;

                try {
                    HashMap<String, String> entry = processLine(data, line);
                    data.totalRows++;
                    if(!entry.isEmpty())
                        data.rowData.add(entry);
                    else
                        data.failedRows++;
                } catch (Exception e) {
                    System.err.println("Failed to parse line " + lineNumber + ": " + e.getMessage());
                    data.badLines.add(line);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Processing input failed on line: %d", lineNumber), e);
        }
    }

    private void processHeaders(CSVData data, String headers) {
        System.out.println("Found header line: " + headers);

        String[] columns = parseCsvLine(headers);
        if(!data.fields.isEmpty()) {
            Iterator<String> fieldNames = data.fields.iterator();
            for (String column : columns) {
                if(!stripQuotes(column).equals(fieldNames.next()))
                    throw new IllegalArgumentException("Input files do not have same headers");
            }
        } else {
            for (String column : columns) {
                String columnName = stripQuotes(column);
                data.fields.add(columnName);
                data.fieldCounts.put(columnName, new AtomicInteger());
            }
        }
    }

    private HashMap<String, String> processLine(CSVData data, String line) {
        String[] parsedLine = CSVParser.parseCsvLine(line);

        HashMap<String, String> entry = new HashMap<>(data.fields.size());

        for(int i=0;i<parsedLine.length; i++) {
            String column = parsedLine[i];
            if(data.fields.size() <= i)
                data.fields.add("Column " + (i+1));
            String field = data.fields.get(i);
            String fieldData = stripQuotes(column);

            if(isSet(fieldData)) {
                entry.put(field, fieldData);
                AtomicInteger count = data.fieldCounts.get(field);
                if(count == null)
                    data.fieldCounts.put(field, new AtomicInteger(1));
                else
                    count.incrementAndGet();
            }
        }
        return entry;
    }

    private String stripQuotes(String input) {
        return (input.length() > 1 && input.startsWith("\"") && input.endsWith("\""))?
            input.substring(1, input.length()-1):
            input;
    }

    public static String[] parseCsvLine(String line) {
        boolean quote=false;

        char[] chars = line.trim().toCharArray();
        List<String> fields = new LinkedList<>();
        int start = 0;
        for(int i=0; i<chars.length; i++) {
            switch (chars[i]) {
                case ',':
                    if(quote)
                        break;

                    String field = new String(chars, start, i-start).trim();
                    fields.add(field);
                    start=i+1;
                    break;
                case '\"':
                    quote = !quote;
                    break;
            }
        }
        if(start < chars.length) {
            if(quote)
                throw new RuntimeException("Could not parse csv line.");

            String field = new String(chars, start, chars.length-start).trim();
            fields.add(field);
        } else {
            //Empty last field
            fields.add("");
        }

        return fields.toArray(new String[fields.size()]);
    }
}
