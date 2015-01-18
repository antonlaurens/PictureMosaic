# PictureMosaic

The project aims at rendering picture mosaics (ie. pictures built out of other pictures), e.g

### Example Input

![alt input](https://github.com/antonlaurens/PictureMosaic/blob/master/examples/input.jpg =100x100)

### Example Output

![alt output](https://github.com/antonlaurens/PictureMosaic/blob/master/examples/output.jpg =100x100)


## Building

```
mvn package
```

For usage, see the command line interface:

## CLI

### Arguments

```
 Option "-blocks (-b)" is required
 java -jar PictureMosaic.jar [options...] arguments...
 -adjacency_ban (-ab) : If set, no two adjacent images can be the same.
 -blocks (-b) N       : The number of tiles per row/ column of the
                        PictureMosaic. Defaults to 50.
 -cache_rebuild (-cr) : When the source images directory is read for the first
                        time, a cache file is created to speed up consequent
                        read. To force a rebuild of the image cache, specify
                        this argument.
 -circle (-cir)       : If this is set, then tiles will be drawn as circles,
                        not rectangles.
 -consume (-c)        : If set, then a source image can only be used once as a
                        tile in the PictureMosaic. Please note that you run the
                        risk of running out of photos.
 -dir (-d) VAL        : The directory in which the source images are located.
                        These images will be used to build the PictureMosaic
                        from, the smaller these images are the better.
 -input (-i) VAL      : Input filename.
 -noise (-n) N        : Adds a chance of noise to the mosaic: [0, 1.0].
                        Defaults to zero.
 -output (-o) VAL     : Output filename.
 -padding (-p) N      : The amount of padding in pixels between tiles.
 -stroke (-s) N       : The stroke width on a tile. The colour of the stroke is
                        the average RGB values inside the image.
 -tint (-t) N         : Indicates the alpha of the color to tint the blocks
                        with: [0, 255]. Defaults to 0.
 -verbose (-v)        : Enables verbose output.

  Example: java -jar PictureMosaic.jar -blocks (-b) N -dir (-d) VAL -input (-i) VAL -output (-o) VAL
```