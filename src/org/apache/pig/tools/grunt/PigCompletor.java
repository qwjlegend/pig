/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.tools.grunt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jline.Completor;

public class PigCompletor implements Completor {
    private final Log log = LogFactory.getLog(getClass());
    Set<String> candidates;
    static final String AUTOCOMPLETE_FILENAME = "autocomplete";

    public PigCompletor() {
        candidates = new TreeSet<String>();
        try {
            InputStream keywordStream;
            
            // try to find keyword file in current directory
            keywordStream = null;
            try
            {
                keywordStream = new FileInputStream(AUTOCOMPLETE_FILENAME);
            }
            catch (FileNotFoundException e)
            {
                log.debug("Can not find autocomplete file in current directory, skipped");
            }
            if (keywordStream!=null)
            {
                processKeywordStream(keywordStream);
                keywordStream.close();
            }
            
            // try to find all keyword file in CLASSPATH
            Enumeration<URL> itr = getClass().getClassLoader().getResources(AUTOCOMPLETE_FILENAME);
            while (itr.hasMoreElements())
            {
                URL url = itr.nextElement();
                keywordStream = url.openStream();
                if (keywordStream!=null)
                {
                    processKeywordStream(keywordStream);
                    keywordStream.close();
                }
                else
                    log.debug("Error loading " + url + ", skipped");
            }
            
            // try to use default keyword file
            keywordStream = getClass().getResourceAsStream("/org/apache/pig/tools/grunt/autocomplete");
            if (keywordStream!=null)
            {
                processKeywordStream(keywordStream);
                keywordStream.close();
            }
        }
        catch (IOException e) {
            log.warn("Error occurs when reading internal autocomplete file, skipped");
        }
    }

    private void processKeywordStream(InputStream stream) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line=reader.readLine())!=null)
        {
            if (!line.startsWith("#")&&!line.startsWith("//"))
            {
                line = line.trim();
                if (!line.equals(""))
                    addCandidateString(line);
            }
                
        }
        reader.close();
    }

    public void addCandidateString(final String candidateString) {
        for (int i=0;i<candidateString.length();i++)
        {
            if (isDelimit(candidateString.charAt(i)))
            {
                log.warn("Auto-complete keyword \"" + candidateString + "\" is skipped because it contains white spaces");
                return;
            }
        }
        candidates.add(candidateString);
    }

    public int complete(String buffer, int cursor, List candidates) {
        if (cursor == 0)
            return 0;
        int p = cursor;
        p--;
        while (p > 0) {
            char c = buffer.charAt(p);
            if (isDelimit(c)) {
                p++;
                break;
            }
            p--;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = p; i < cursor; i++) {
            sb.append(buffer.charAt(i));
        }
        if (!sb.toString().equals("")) {
            List<String> matches = searchCandidate(sb.toString());
            if (matches != null) {
                for (String match:matches)
                    candidates.add(match);
                return p;
            }
        }
        return 0;
    }

    private boolean isDelimit(char c) {
        if (Character.isWhitespace(c))
            return true;
        return false;
    }

    private List<String> searchCandidate(String s) {
        List<String> list = new ArrayList<String>();
        for (String can : candidates) {
            if (can.startsWith(s) && !can.equals(s))
                list.add(can);
        }
        return list;
    }
}
