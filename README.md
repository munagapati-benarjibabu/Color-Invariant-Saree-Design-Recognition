# Saree AI Search

A complete Java 17 / Spring Boot application that searches a local saree collection by dominant colour. It uses OpenCV to convert images to HSV, classifies significant colours, stores the top two colour signals in MySQL, and ranks the collection by colour and percentage similarity.

## Included dataset

The project has been populated from the supplied folder with 132 JPG/JPEG saree images. They are copied under `saree-images/` with their source colour folders preserved. You can add more JPG, JPEG, or PNG files anywhere under this directory; the reindex operation finds them recursively.

## Architecture

`HTML/CSS/JavaScript → POST /api/sarees/search → SareeController → SareeService → OpenCV HSV analysis → MySQL SareeRepository → similarity-ranked JSON → gallery`

The colour detector samples a resized image in HSV space, separates neutral colours by saturation/value, classifies chromatic pixels by hue ranges, and returns the three strongest colours. Matching compares hue closeness and percentage difference—rather than matching colour labels alone. Change `saree.match.threshold=70` in `src/main/resources/application.properties` to tune the minimum score.

## macOS (Apple Silicon) setup

1. Install prerequisites:

   ```bash
   brew install openjdk@17 maven mysql
   brew services start mysql
   ```

2. Create the database and set a MySQL password if needed:

   ```bash
   mysql -u root -p < database/schema.sql
   ```

3. Edit `src/main/resources/application.properties` and replace `YOUR_PASSWORD` with your MySQL password. The supplied JavaCPP OpenCV dependency downloads platform-native binaries automatically, so a separate OpenCV install is not required.

4. Build and run:

   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

5. In another terminal, populate (or refresh) the MySQL colour index:

   ```bash
   curl -X POST http://localhost:8080/api/sarees/reindex
   ```

6. Open [http://localhost:8080](http://localhost:8080), choose a JPG/JPEG/PNG saree image, and select **Search sarees**.

## APIs

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/sarees/search` | multipart field `image`; returns detected colours and ranked matches |
| GET | `/api/sarees` | list indexed sarees |
| GET | `/api/sarees/{id}` | get one saree |
| POST | `/api/sarees/reindex` | scan `saree-images/` and insert/update colour metadata |

The local images are served at `/saree-images/**` by `WebConfig`; only file paths and colour metadata are stored in MySQL, not image blobs.

## Testing

- Upload a red saree: red matches should appear after indexing.
- Upload a blue saree: blue matches should appear when blue sarees are in the dataset.
- Upload a colour absent from the collection: the page shows “No matching saree found in the database.”
- Upload a PDF or other invalid file: the page rejects it with the supported-format message.

## Troubleshooting

- **Database connection error:** verify MySQL is running and the password in `application.properties` is correct.
- **No results from every search:** call `POST /api/sarees/reindex`, then confirm it reports a non-zero indexed count.
- **A newly added image does not appear:** put it under `saree-images/`, then run reindex again.
- **Maven cannot find Java 17:** run `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` before Maven.

## Future AI upgrade

The controller response and repository boundary allow an additional image-embedding service later. Add CLIP or Sentence Transformers embeddings, save vectors in Milvus, FAISS, or Qdrant, then combine vector similarity with this colour score to search design, pattern, and fabric texture as well as colour.
