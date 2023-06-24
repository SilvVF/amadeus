package io.silv.amadeus.network.mangadex.models.test

object MangaDexTestJson {

    const val chapter = "{\n" +
            "  \"result\": \"ok\",\n" +
            "  \"response\": \"collection\",\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "      \"type\": \"chapter\",\n" +
            "      \"attributes\": {\n" +
            "        \"title\": \"string\",\n" +
            "        \"volume\": \"string\",\n" +
            "        \"chapter\": \"string\",\n" +
            "        \"pages\": 0,\n" +
            "        \"translatedLanguage\": \"string\",\n" +
            "        \"uploader\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "        \"externalUrl\": \"string\",\n" +
            "        \"version\": 1,\n" +
            "        \"createdAt\": \"string\",\n" +
            "        \"updatedAt\": \"string\",\n" +
            "        \"publishAt\": \"string\",\n" +
            "        \"readableAt\": \"string\"\n" +
            "      },\n" +
            "      \"relationships\": [\n" +
            "        {\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"related\": \"monochrome\",\n" +
            "          \"attributes\": {}\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"limit\": 0,\n" +
            "  \"offset\": 0,\n" +
            "  \"total\": 0\n" +
            "}"

    const val chapter_id = "{\n" +
            "  \"result\": \"ok\",\n" +
            "  \"response\": \"entity\",\n" +
            "  \"data\": {\n" +
            "    \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "    \"type\": \"chapter\",\n" +
            "    \"attributes\": {\n" +
            "      \"title\": \"string\",\n" +
            "      \"volume\": \"string\",\n" +
            "      \"chapter\": \"string\",\n" +
            "      \"pages\": 0,\n" +
            "      \"translatedLanguage\": \"string\",\n" +
            "      \"uploader\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "      \"externalUrl\": \"string\",\n" +
            "      \"version\": 1,\n" +
            "      \"createdAt\": \"string\",\n" +
            "      \"updatedAt\": \"string\",\n" +
            "      \"publishAt\": \"string\",\n" +
            "      \"readableAt\": \"string\"\n" +
            "    },\n" +
            "    \"relationships\": [\n" +
            "      {\n" +
            "        \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "        \"type\": \"string\",\n" +
            "        \"related\": \"monochrome\",\n" +
            "        \"attributes\": {}\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}"

    const val manga_id_aggregate = "{\n" +
            "  \"result\": \"ok\",\n" +
            "  \"volumes\": {\n" +
            "    \"additionalProp1\": {\n" +
            "      \"volume\": \"string\",\n" +
            "      \"count\": 0,\n" +
            "      \"chapters\": {\n" +
            "        \"additionalProp1\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp2\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp3\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"additionalProp2\": {\n" +
            "      \"volume\": \"string\",\n" +
            "      \"count\": 0,\n" +
            "      \"chapters\": {\n" +
            "        \"additionalProp1\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp2\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp3\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"additionalProp3\": {\n" +
            "      \"volume\": \"string\",\n" +
            "      \"count\": 0,\n" +
            "      \"chapters\": {\n" +
            "        \"additionalProp1\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp2\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        },\n" +
            "        \"additionalProp3\": {\n" +
            "          \"chapter\": \"string\",\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"others\": [\n" +
            "            \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
            "          ],\n" +
            "          \"count\": 0\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}"

    const val manga = "{\n" +
            "  \"result\": \"ok\",\n" +
            "  \"response\": \"collection\",\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "      \"type\": \"manga\",\n" +
            "      \"attributes\": {\n" +
            "        \"title\": {\n" +
            "          \"additionalProp1\": \"string\",\n" +
            "          \"additionalProp2\": \"string\",\n" +
            "          \"additionalProp3\": \"string\"\n" +
            "        },\n" +
            "        \"altTitles\": [\n" +
            "          {\n" +
            "            \"additionalProp1\": \"string\",\n" +
            "            \"additionalProp2\": \"string\",\n" +
            "            \"additionalProp3\": \"string\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"description\": {\n" +
            "          \"additionalProp1\": \"string\",\n" +
            "          \"additionalProp2\": \"string\",\n" +
            "          \"additionalProp3\": \"string\"\n" +
            "        },\n" +
            "        \"isLocked\": true,\n" +
            "        \"links\": {\n" +
            "          \"additionalProp1\": \"string\",\n" +
            "          \"additionalProp2\": \"string\",\n" +
            "          \"additionalProp3\": \"string\"\n" +
            "        },\n" +
            "        \"originalLanguage\": \"string\",\n" +
            "        \"lastVolume\": \"string\",\n" +
            "        \"lastChapter\": \"string\",\n" +
            "        \"publicationDemographic\": \"shounen\",\n" +
            "        \"status\": \"completed\",\n" +
            "        \"year\": 0,\n" +
            "        \"contentRating\": \"safe\",\n" +
            "        \"chapterNumbersResetOnNewVolume\": true,\n" +
            "        \"latestUploadedChapter\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "        \"tags\": [\n" +
            "          {\n" +
            "            \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "            \"type\": \"tag\",\n" +
            "            \"attributes\": {\n" +
            "              \"name\": {\n" +
            "                \"additionalProp1\": \"string\",\n" +
            "                \"additionalProp2\": \"string\",\n" +
            "                \"additionalProp3\": \"string\"\n" +
            "              },\n" +
            "              \"description\": {\n" +
            "                \"additionalProp1\": \"string\",\n" +
            "                \"additionalProp2\": \"string\",\n" +
            "                \"additionalProp3\": \"string\"\n" +
            "              },\n" +
            "              \"group\": \"content\",\n" +
            "              \"version\": 1\n" +
            "            },\n" +
            "            \"relationships\": [\n" +
            "              {\n" +
            "                \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "                \"type\": \"string\",\n" +
            "                \"related\": \"monochrome\",\n" +
            "                \"attributes\": {}\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ],\n" +
            "        \"state\": \"draft\",\n" +
            "        \"version\": 1,\n" +
            "        \"createdAt\": \"string\",\n" +
            "        \"updatedAt\": \"string\"\n" +
            "      },\n" +
            "      \"relationships\": [\n" +
            "        {\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"type\": \"string\",\n" +
            "          \"related\": \"monochrome\",\n" +
            "          \"attributes\": {}\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"limit\": 0,\n" +
            "  \"offset\": 0,\n" +
            "  \"total\": 0\n" +
            "}"

    const val manga_id = "{\n" +
            "  \"result\": \"ok\",\n" +
            "  \"response\": \"entity\",\n" +
            "  \"data\": {\n" +
            "    \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "    \"type\": \"manga\",\n" +
            "    \"attributes\": {\n" +
            "      \"title\": {\n" +
            "        \"additionalProp1\": \"string\",\n" +
            "        \"additionalProp2\": \"string\",\n" +
            "        \"additionalProp3\": \"string\"\n" +
            "      },\n" +
            "      \"altTitles\": [\n" +
            "        {\n" +
            "          \"additionalProp1\": \"string\",\n" +
            "          \"additionalProp2\": \"string\",\n" +
            "          \"additionalProp3\": \"string\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"description\": {\n" +
            "        \"additionalProp1\": \"string\",\n" +
            "        \"additionalProp2\": \"string\",\n" +
            "        \"additionalProp3\": \"string\"\n" +
            "      },\n" +
            "      \"isLocked\": true,\n" +
            "      \"links\": {\n" +
            "        \"additionalProp1\": \"string\",\n" +
            "        \"additionalProp2\": \"string\",\n" +
            "        \"additionalProp3\": \"string\"\n" +
            "      },\n" +
            "      \"originalLanguage\": \"string\",\n" +
            "      \"lastVolume\": \"string\",\n" +
            "      \"lastChapter\": \"string\",\n" +
            "      \"publicationDemographic\": \"shounen\",\n" +
            "      \"status\": \"completed\",\n" +
            "      \"year\": 0,\n" +
            "      \"contentRating\": \"safe\",\n" +
            "      \"chapterNumbersResetOnNewVolume\": true,\n" +
            "      \"latestUploadedChapter\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "      \"tags\": [\n" +
            "        {\n" +
            "          \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "          \"type\": \"tag\",\n" +
            "          \"attributes\": {\n" +
            "            \"name\": {\n" +
            "              \"additionalProp1\": \"string\",\n" +
            "              \"additionalProp2\": \"string\",\n" +
            "              \"additionalProp3\": \"string\"\n" +
            "            },\n" +
            "            \"description\": {\n" +
            "              \"additionalProp1\": \"string\",\n" +
            "              \"additionalProp2\": \"string\",\n" +
            "              \"additionalProp3\": \"string\"\n" +
            "            },\n" +
            "            \"group\": \"content\",\n" +
            "            \"version\": 1\n" +
            "          },\n" +
            "          \"relationships\": [\n" +
            "            {\n" +
            "              \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "              \"type\": \"string\",\n" +
            "              \"related\": \"monochrome\",\n" +
            "              \"attributes\": {}\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"state\": \"draft\",\n" +
            "      \"version\": 1,\n" +
            "      \"createdAt\": \"string\",\n" +
            "      \"updatedAt\": \"string\"\n" +
            "    },\n" +
            "    \"relationships\": [\n" +
            "      {\n" +
            "        \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
            "        \"type\": \"string\",\n" +
            "        \"related\": \"monochrome\",\n" +
            "        \"attributes\": {}\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}"
}