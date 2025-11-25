import json
import os
from src import config, edges, clustering, queue_manager

def load_metadata():
    print(f"Завантаження метаданих з {config.METADATA_FILE}...")
    if not os.path.exists(config.METADATA_FILE):
        raise FileNotFoundError("Файл metadata.json не знайдено! Покладіть його в папку data/")
    
    with open(config.METADATA_FILE, 'r', encoding='utf-8') as f:
        return json.load(f)

def main():
    # 1. Завантаження
    metadata = load_metadata()
    
    # 2. Обчислення ребер
    edge_list = edges.calculate_edges(metadata)
    edges.save_edges(edge_list, config.EDGES_FILE)
    
    # 3. Кластеризація
    node_to_cluster = clustering.perform_clustering(edge_list)
    clustering.save_clusters(node_to_cluster, config.CLUSTERS_FILE)
    
    # 4. Створення черги
    queue = queue_manager.build_queue(metadata, node_to_cluster)
    queue_manager.save_queue(queue, config.QUEUE_FILE)
    
    print("\n=== Готово! ===")
    print("Ви можете знайти evaluation_queue.json у папці data/")

if __name__ == "__main__":
    main()
