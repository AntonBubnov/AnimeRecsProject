import networkx as nx
from cdlib import algorithms
import json

def perform_clustering(edges_data):
    """
    Будує граф та виконує кластеризацію.
    """
    print("Побудова графу та кластеризація...")
    
    G = nx.Graph()
    
    # Додаємо ребра в граф
    for edge in edges_data:
        G.add_edge(edge['source'], edge['target'], weight=edge['weight'])
    
    print(f"Граф побудовано: {G.number_of_nodes()} вузлів, {G.number_of_edges()} ребер.")
    
    # Використовуємо алгоритм Louvain (швидкий і якісний)
    # Можна замінити на algorithms.infomap(G), як в оригіналі, якщо встановлено
    coms = algorithms.louvain(G, weight='weight')
    
    # Отримуємо словник {node_id: cluster_id}
    node_to_cluster = {}
    for cluster_id, community in enumerate(coms.communities):
        for node in community:
            node_to_cluster[node] = cluster_id
            
    print(f"Знайдено {len(coms.communities)} кластерів.")
    return node_to_cluster

def save_clusters(node_to_cluster, filepath):
    # Конвертуємо ключі в стрічки для JSON
    data = {str(k): v for k, v in node_to_cluster.items()}
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)
    print(f"Кластери збережено у {filepath}")
