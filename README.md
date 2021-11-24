# BaritoneCalculator
Calculator of path from [Baritone](https://github.com/cabaletta/baritone).

### How it works ?

You set destination, and it will calculate easier way to go.

### From Baritone ?

Yes. The Baritone project is open source. I just take the calculation that I need, and keep header/package.

### Some feature are not here !

Yes, block break/place isn't take in count. And I think this will be added soon.

---
## No download link !

Yes. It doesn't work alone. It's a library that should be included in others.

## As developer, how can I use ?

You should start the goal to the given direction :

```java
Goal goal = new GoalBlock(x, y, z); // create direction
BaritoneAPI.getProvider().getBaritone(p).getPathingBehavior().startGoal(goal); // start goal
```
Here it will calculate the path. To get it, do this :
```java
Optional<IPath> path = BaritoneAPI.getProvider().getBaritone(p).getPathingBehavior().getPath(); // get optional path
if(path.isPresent()) { // if path calculated
   List<BetterBlockPos> pos = path.get().positions(); // get positions
   // now you have all block that player should pass through to go where he wants
} else {
   // no path founded
}
```

## It failed to do what I want !

You can enable debugging :
```java
BaritoneAPI.setPlugin(myPluginInstance); // this is important : used for logger
BaritoneAPI.setDebug(true); // enable debug
```

## Dependencies ?

The alone dependency is spigot. You can use spigot 1.8 to spigot 1.17. There is no maven support yet.
