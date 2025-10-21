import osmnx as ox
import networkx as nx
import geopandas as gpd
from shapely.geometry import Point
import folium
import numpy as np
import rasterio
from rasterio.transform import from_origin

ox.config(use_cache=True, log_console=True)

place_name = "Moscow, Russia"
G = ox.graph_from_place(place_name, network_type='drive')

G = ox.add_edge_speeds(G)
G = ox.add_edge_travel_times(G)

orig_point = (55.753930, 37.620795)  # Координаты Красной площади
orig_node = ox.distance.nearest_nodes(G, orig_point[1], orig_point[0])


def create_isochrone_data(G, orig_node, time_range=[5, 10, 15, 20, 25, 30]):
    time_range = [t * 60 for t in time_range]  # Преобразование времени в секунды
    all_isochrones = []

    for t in time_range:
        subgraph = nx.ego_graph(G, orig_node, radius=t, distance='travel_time')
        isochrone_nodes = set(subgraph.nodes())
        node_points = [Point(G.nodes[node]['x'], G.nodes[node]['y']) for node in isochrone_nodes]
        gdf_nodes = gpd.GeoSeries(node_points)
        gdf_convex = gdf_nodes.unary_union.convex_hull
        all_isochrones.append(gdf_convex)

    return all_isochrones


isochrones = create_isochrone_data(G, orig_node)

gdf_isochrones = gpd.GeoDataFrame(geometry=isochrones)
gdf_isochrones.to_file("isochrones.shp")


def save_geotiff(isochrones, filename='travel_time_surface.tif'):
    width, height = 1000, 1000  # Размер растра
    transform = from_origin(37.5, 55.9, 0.001, 0.001)  # Преобразование координат

    # Создаем пустую матрицу для растровых данных
    raster_data = np.zeros((height, width), dtype=np.uint8)

    # Заполняем матрицу значениями для каждой зоны
    for i, isochrone in enumerate(isochrones, start=1):
        minx, miny, maxx, maxy = isochrone.bounds
        # Рассчитаем индекс в массиве для текущей зоны
        ix_min = int((minx - 37.5) / 0.001)
        iy_min = int((55.9 - maxy) / 0.001)
        ix_max = int((maxx - 37.5) / 0.001)
        iy_max = int((55.9 - miny) / 0.001)

        # Присвоим каждому пикселю в пределах зоны значение i
        raster_data[iy_min:iy_max, ix_min:ix_max] = i

    # Сохранение в GeoTIFF
    with rasterio.open(
            filename, 'w', driver='GTiff',
            height=height, width=width,
            count=1, dtype=raster_data.dtype,
            crs='+proj=latlong', transform=transform,
    ) as dst:
        dst.write(raster_data, 1)


save_geotiff(isochrones)


def save_isochrone_map(G, orig_point, isochrones):
    m = folium.Map(location=[orig_point[0], orig_point[1]], zoom_start=12)

    for isochrone in isochrones:
        folium.GeoJson(isochrone.__geo_interface__, style_function=lambda x: {
            'fillColor': '#ff0000',
            'color': '#ff0000',
            'weight': 2,
            'fillOpacity': 0.5
        }).add_to(m)

    m.save('moscow_isochrones.html')

    print("Карта сохранена в формате HTML")


save_isochrone_map(G, orig_point, isochrones)
