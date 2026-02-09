package com.brimmatech.docflow.v2.constants;

import com.fasterxml.jackson.databind.JsonNode;

public class JqScriptVault {


    public static String getFieldExtractJqScript() {
        return String.format("{fields: .fields}");
    }

    public static String getContentFieldExtractJqScript() {
        return String.format("{fields: .fields}");
    }



    /**
     * Trasnforms AnalyszeResults format into something like
     * [
     *   {
     *     "docType": "hazard_insurance_binder",
     *     "pages": [
     *       1,
     *       2,
     *       3,
     *       4,
     *       5
     *     ]
     *   },...
     *   ]
     * @return
     */
    public static String getClassificationPageStructure() {
        return String.format(".documents | map ({ docType: .docType, pages: .boundingRegions | map(.pageNumber) })");
    }

    public static Result countFieldsAndBoundingRegions(JsonNode node) {
        if (node == null || node.isNull()) {
            return new Result(0, 0);
        }

        int totalFields = 0;
        int fieldsWithBoundingRegions = 0;

        if (node.has("type")) {
            String type = node.get("type").asText();

            if (!type.equals("object") && !type.equals("array")) {
                totalFields++;
                if (node.has("boundingRegions") && node.get("boundingRegions").isArray()) {
                    fieldsWithBoundingRegions++;
                }
            } else {
                JsonNode valueObject = node.get("valueObject");
                JsonNode valueArray = node.get("valueArray");

                if (valueObject != null && valueObject.isObject()) {
                    Result childResult = countFieldsAndBoundingRegions(valueObject);
                    totalFields += childResult.totalFields;
                    fieldsWithBoundingRegions += childResult.fieldsWithBoundingRegions;
                }

                if (valueArray != null && valueArray.isArray()) {
                    for (JsonNode arrayElement : valueArray) {
                        Result childResult = countFieldsAndBoundingRegions(arrayElement);
                        totalFields += childResult.totalFields;
                        fieldsWithBoundingRegions += childResult.fieldsWithBoundingRegions;


                    }
                }
            }

        } else {
            for (JsonNode child : node) {
                Result childResult = countFieldsAndBoundingRegions(child);
                totalFields += childResult.totalFields;
                fieldsWithBoundingRegions += childResult.fieldsWithBoundingRegions;
            }
        }

        return new Result(totalFields, fieldsWithBoundingRegions);
    }

    public static class Result {
        int totalFields;
        int fieldsWithBoundingRegions;

        Result(int totalFields, int fieldsWithBoundingRegions) {
            this.totalFields = totalFields;
            this.fieldsWithBoundingRegions = fieldsWithBoundingRegions;
        }

        public int getTotalFields() {
            return totalFields;
        }

        public int getFieldsWithBoundingRegions() {
            return fieldsWithBoundingRegions;
        }
    }
}
