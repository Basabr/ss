package it.polito.extgol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer width = 5;

    @Column(nullable = false)
    private Integer height = 5;

    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private Game game;

    @OneToMany(
        mappedBy = "board",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @MapKey(name = "tileCoord")
    private Map<Coord, Tile> tiles = new HashMap<>();

    public Board() {}

    public Board(int width, int height, Game g) {
        this.width = width;
        this.height = height;
        this.game = g;
        initializeTiles();
    }

    public static Board createExtended(int width, int height, Game game) {
        Board board = new Board(width, height, game);

        for (Tile t : board.getTiles()) {
            Board.setInteractableTile(board, t.getCoordinates(), 0);
        }

        for (Cell c : board.getCellSet()) {
            c.setMood(CellMood.NAIVE);
            c.setType(CellType.BASIC);
        }

        return board;
    }

    private void initializeTiles() {
        tiles.clear();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = new Tile(x, y, this, this.game);
                tile.setBoard(this);
                tiles.put(tile.getCoordinates(), tile);
            }
        }
        for (Tile t : tiles.values()) {
            t.initializeNeighbors(getAdjacentTiles(t));
        }
    }

    public Set<Tile> getAdjacentTiles(Tile tile) {
        Set<Tile> adj = new HashSet<>();
        int cx = tile.getX();
        int cy = tile.getY();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Tile t = getTile(new Coord(cx + dx, cy + dy));
                if (t != null) {
                    adj.add(t);
                }
            }
        }
        return adj;
    }

    public Integer getId() {
        return id;
    }

    public Tile getTile(Coord c) {
        return tiles.get(c);
    }

    public List<Tile> getTiles() {
        return List.copyOf(tiles.values());
    }

    public Set<Cell> getCellSet() {
        Set<Cell> cellSet = new HashSet<>();
        for (Tile t : tiles.values()) {
            if (t.getCell() != null) {
                cellSet.add(t.getCell());
            }
        }
        return cellSet;
    }

    public String visualize(Generation generation) {
        Set<Coord> alive = generation.getAliveCells().stream()
            .map(Cell::getCoordinates)
            .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                sb.append(alive.contains(new Coord(x, y)) ? 'C' : '0');
            }
            if (y < height - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    // --- متد مهم برای R3 ---

    public static InteractableTile setInteractableTile(Board board, Coord coord, Integer lifePointsModifier) {
        Tile oldTile = board.getTile(coord);
        if (oldTile == null) return null;

        // حذف تایل قبلی از نقشه
        board.tiles.remove(coord);

        // ساخت تایل تعاملی جدید
        InteractableTile newTile = new InteractableTile(coord.getX(), coord.getY(), board, board.game, lifePointsModifier);
        newTile.setBoard(board);

        // اضافه کردن تایل جدید به نقشه
        board.tiles.put(coord, newTile);

        // مقداردهی همسایه‌ها برای تایل جدید
        newTile.initializeNeighbors(board.getAdjacentTiles(newTile));

        // به‌روزرسانی همسایه‌ها برای همسایگان اطراف
        for (Tile neighbor : board.getAdjacentTiles(newTile)) {
            neighbor.initializeNeighbors(board.getAdjacentTiles(neighbor));
        }

        return newTile;
    }

    // --- متدهای آمار و گزارش ---

    public Integer countCells(Generation generation) {
        return (int) generation.getAliveCells().stream()
                .filter(Cell::isAlive)
                .count();
    }

    public Cell getHighestEnergyCell(Generation gen) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .sorted((c1, c2) -> {
                    int diff = c2.getLifePoints() - c1.getLifePoints();
                    if (diff != 0) return diff;
                    int cmpY = Integer.compare(c1.getY(), c2.getY());
                    if (cmpY != 0) return cmpY;
                    return Integer.compare(c1.getX(), c2.getX());
                })
                .findFirst()
                .orElse(null);
    }

    public Map<Integer, List<Cell>> getCellsByEnergyLevel(Generation gen) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .collect(Collectors.groupingBy(Cell::getLifePoints));
    }

    public Map<CellType, Integer> countCellsByType(Generation gen) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .collect(Collectors.groupingBy(Cell::getType, Collectors.summingInt(c -> 1)));
    }

    public List<Cell> topEnergyCells(Generation gen, int n) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .sorted((c1, c2) -> c2.getLifePoints() - c1.getLifePoints())
                .limit(n)
                .collect(Collectors.toList());
    }

    public Map<Integer, List<Cell>> groupByAliveNeighborCount(Generation gen) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .collect(Collectors.groupingBy(Cell::countAliveNeighbors));
    }

    public IntSummaryStatistics energyStatistics(Generation gen) {
        return gen.getAliveCells().stream()
                .filter(Cell::isAlive)
                .collect(Collectors.summarizingInt(Cell::getLifePoints));
    }

    public Map<Integer, IntSummaryStatistics> getTimeSeriesStats(int fromStep, int toStep) {
        Map<Integer, IntSummaryStatistics> stats = new HashMap<>();
        if (game == null) return stats;
        List<Generation> generations = game.getGenerations();

        for (int step = fromStep; step <= toStep; step++) {
            if (step < 0 || step >= generations.size()) continue;
            Generation gen = generations.get(step);
            stats.put(step, energyStatistics(gen));
        }
        return stats;
    }
}
