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

```java



public class Board {
    private final int width;
    private final int height;
    private final Map<Coord, Tile> tiles = new HashMap<>();

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        // initialize tiles with default Tile instances
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles.put(new Coord(x, y), new Tile(this, x, y));
            }
        }
    }

    public Tile getTile(int x, int y) {
        return tiles.get(new Coord(x, y));
    }

    public Tile getTile(Coord c) {
        return tiles.get(c);
    }
}

// Assuming InteractableTile is a subclass of Tile
class InteractableTile extends Tile {
    public InteractableTile(Board board, int x, int y) {
        super(board, x, y);
    }

    // additional InteractableTile methods here
}

// Assuming Cell class with getTile method
class Cell {
    private final int x;
    private final int y;
    private final Board board;
    private final CellType cellType;

    public Cell(int x, int y, Board board, CellType cellType) {
        this.x = x;
        this.y = y;
        this.board = board;
        this.cellType = cellType;
    }

    public Tile getTile() {
        return board.getTile(x, y);
    }
}

// Support classes used in Board.java
class Coord {
    public final int x, y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coord)) return false;
        Coord coord = (Coord) o;
        return x == coord.x && y == coord.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}

class Tile {
    protected final Board board;
    protected final int x;
    protected final int y;

    public Tile(Board board, int x, int y) {
        this.board = board;
        this.x = x;
        this.y = y;
    }

    // Tile methods here
}

enum CellType {
    NAIVE, VAMPIRE, HEALER;
}
```

