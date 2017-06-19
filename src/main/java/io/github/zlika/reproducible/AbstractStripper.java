package io.github.zlika.reproducible;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractStripper implements Stripper {
	
    private final Map<String, Stripper> subFilters = new HashMap<>();
    
    /**
     * Adds a stripper for a given file in the Zip.
     * @param filename the name of the file in the Zip (regular expression).
     * @param stripper the stripper to apply on the file.
     * @return this object (for method chaining).
     */
    public AbstractStripper addFileStripper(String filename, Stripper stripper)
    {
        subFilters.put(filename, stripper);
        return this;
    }
    
    protected Stripper getSubFilter(String name)
    {
        for (Entry<String, Stripper> filter : subFilters.entrySet())
        {
            if (name.matches(filter.getKey()))
            {
                return filter.getValue();
            }
        }
        return null;
    }
    
    
}
