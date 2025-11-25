import json
from collections import defaultdict

def build_queue(metadata, node_to_cluster):
    """
    Створює чергу на основі популярності всередині кожного кластера.
    """
    print("Генерація черги оцінювання...")
    
    # 1. Групуємо аніме по кластерах
    clusters = defaultdict(list)
    
    for anime_id_str, data in metadata.items():
        anime_id = int(anime_id_str)
        
        # Якщо аніме немає в кластерах (не мало ребер), пропускаємо або кидаємо в "інше"
        cluster_id = node_to_cluster.get(anime_id)
        
        if cluster_id is not None:
            # Витягуємо популярність (кількість member-ів)
            # В metadata.json це поле часто називається 'members' або 'num_list_users'
            # Перевірте точну назву поля у вашому JSON
            popularity = data.get('members', 0) or data.get('num_list_users', 0)
            
            clusters[cluster_id].append({
                "id": anime_id,
                "title": data.get('title', 'Unknown'),
                "cluster_id": cluster_id,
                "popularity": popularity
            })
            
    # 2. Сортуємо всередині кластерів
    sorted_clusters = {}
    for cid, items in clusters.items():
        sorted_clusters[cid] = sorted(items, key=lambda x: x['popularity'], reverse=True)
        
    # 3. Інтерлівінг (Interleaving) - беремо по одному з кожного кластера
    queue = []
    max_len = max(len(c) for c in sorted_clusters.values()) if sorted_clusters else 0
    
    # Сортуємо самі кластери, щоб спочатку йшли ті, де сумарна популярність вища (опціонально)
    cluster_ids = sorted(sorted_clusters.keys())
    
    for i in range(max_len):
        for cid in cluster_ids:
            if i < len(sorted_clusters[cid]):
                queue.append(sorted_clusters[cid][i])
                
    print(f"Черга створена. Довжина: {len(queue)}")
    return queue

def save_queue(queue, filepath):
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(queue, f, indent=2, ensure_ascii=False)
    print(f"Чергу збережено у {filepath}")
