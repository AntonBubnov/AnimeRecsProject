import json
from tqdm import tqdm
from .config import MIN_EDGE_WEIGHT

def calculate_edges(metadata):
    """
    Конвертує метадані у список ребер.
    Адаптовано для читання raw-структури MAL API (node.id та num_recommendations).
    """
    print("Обчислення ребер графу...")
    edges = []
    
    # metadata - це словник {anime_id: data}
    for anime_id, data in tqdm(metadata.items(), desc="Processing Anime"):
        recommendations = data.get('recommendations', [])
        
        if not recommendations:
            continue
            
        if isinstance(recommendations, dict):
            recs_list = list(recommendations.values())
        else:
            recs_list = recommendations

        # --- ВИПРАВЛЕННЯ ТУТ ---
        # Рахуємо загальну кількість рекомендацій.
        # Ми перевіряємо два варіанти ключів, щоб код працював і з сирими, і з обробленими даними.
        total_recs_count = 0
        for rec in recs_list:
            # Варіант 1: Raw MAL API
            if 'num_recommendations' in rec:
                total_recs_count += rec['num_recommendations']
            # Варіант 2: Processed/Simple format
            elif 'count' in rec:
                total_recs_count += rec['count']
        
        if total_recs_count == 0:
            continue

        source_id = int(anime_id)
        
        for rec in recs_list:
            target_id = None
            count = 0
            
            # Витягуємо ID та Count залежно від формату
            if 'node' in rec and 'id' in rec['node']:
                target_id = rec['node']['id']
                count = rec.get('num_recommendations', 0)
            elif 'id' in rec:
                target_id = rec['id']
                count = rec.get('count', 0)
            
            if target_id is None or count == 0:
                continue
            
            # Логіка зважування (як у recs.ts)
            weight = count / total_recs_count / 2
            
            if weight > MIN_EDGE_WEIGHT:
                edges.append({
                    "source": source_id,
                    "target": int(target_id),
                    "weight": round(weight, 4)
                })
                
    print(f"Знайдено {len(edges)} ребер.")
    return edges

def save_edges(edges, filepath):
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(edges, f, indent=2)
    print(f"Ребра збережено у {filepath}")
