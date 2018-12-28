package csv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CSVParser {

    private final String path = "src/main/resources/files/";

    public List<Map<String, String>> csvParser(String fileName, char separator) {

        List<Map<String, String>> list = new ArrayList<>();
        try (InputStream in = new FileInputStream(path + fileName)) {
            CSV csv = new CSV(true, separator, in);
            List<String> fieldNames = null;
            if (csv.hasNext()) fieldNames = new ArrayList<>(csv.next());

            while (csv.hasNext()) {
                List<String> x = csv.next();
                Map<String, String> obj = new LinkedHashMap<>();
                for (int i = 0; i < fieldNames.size(); i++) {
                    obj.put(fieldNames.get(i), x.get(i));
                }
                list.add(obj);
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(System.out, list);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return list;
    }
}
