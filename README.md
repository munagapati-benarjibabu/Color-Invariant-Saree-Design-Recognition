# Saree AI — colour-invariant visual search

Search a saree catalogue from an uploaded image using a foreground-biased visual descriptor. Results use design/texture similarity first, with colour as an optional signal. The app keeps image analysis on the server; it does not send customer images to a third-party AI service.

## What is included

- **Design-based retrieval:** a compact, colour-invariant luminance-edge and texture signature makes different dye colours comparable.
- **Foreground-aware analysis:** a conservative central crop and studio-background suppression reduce influence from borders, mannequins, and white backdrops.
- **Colour swap:** select *Same design, different colour* or use the action on a result card.
- **Metadata filters:** type, fabric, pattern, occasion, price, and availability.
- **Explanations:** every result reports its design/texture and colour contributions.
- **Catalogue tooling:** protected image upload and metadata endpoints; reindexing records a signature and starter metadata.
- **Evaluation dashboard API:** reports catalogue, signature, and metadata coverage.
- **Production basics:** Docker Compose, environment-based secrets, health endpoint, CI workflow, request limits, and protected write endpoints.

> This version deliberately uses an on-device/server visual descriptor rather than claiming semantic CLIP recognition. For stronger semantic pattern search, replace the descriptor through a CLIP/SigLIP embedding provider and a vector database; the persisted `design_signature` boundary is designed for that upgrade.

## Run locally

```bash
cp .env.example .env
# Set DB_PASSWORD and a long SAREE_ADMIN_TOKEN in .env
mvn clean package
docker compose up --build
```

Or run MySQL yourself and start with `mvn spring-boot:run`. Database settings are read from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; no credential is committed to the repository.

Open http://localhost:8080. Rebuild the starter index after first startup:

```bash
curl -X POST http://localhost:8080/api/sarees/reindex \
  -H "X-Admin-Token: $SAREE_ADMIN_TOKEN"
```

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/sarees/search` | Multipart image search; accepts optional `sareeType`, `fabric`, `pattern`, `occasion`, `maxPrice`, `inStockOnly`, `colourMode=balanced|swap` |
| GET | `/api/sarees/{id}/similar?colourMode=swap` | Similar-design or different-colour recommendations |
| PUT | `/api/sarees/{id}/metadata` | Protected catalogue metadata update |
| POST | `/api/sarees/admin/upload` | Protected multipart image upload plus metadata |
| POST | `/api/sarees/reindex` | Protected full image reindex |
| GET | `/api/sarees/evaluation/report` | Catalogue-quality metrics |
| GET | `/actuator/health` | Deployment health probe |

Send `X-Admin-Token` to every write endpoint. Example metadata payload:

```json
{"sareeType":"Kanjivaram","fabric":"Silk","pattern":"Floral","occasion":"Wedding","price":8999,"inStock":true}
```

## Evaluation

The report endpoint measures metadata and design-signature coverage. For retrieval quality, maintain a small labelled set of query image → relevant catalogue IDs and track Top-1/Top-5 relevance before changing the descriptor or match threshold.
