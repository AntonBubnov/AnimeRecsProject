import os

# Базова директорія проекту
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, 'data')

# Шляхи до файлів
METADATA_FILE = os.path.join(DATA_DIR, 'metadata.json')
EDGES_FILE = os.path.join(DATA_DIR, 'edges.json')
CLUSTERS_FILE = os.path.join(DATA_DIR, 'clusters.json')
QUEUE_FILE = os.path.join(DATA_DIR, 'evaluation_queue.json')

# Налаштування
MIN_EDGE_WEIGHT = 0.03  # Як в оригінальному recs.ts
