# BaritoneCalculator
Calculator of path from [Baritone](https://github.com/cabaletta/baritone).

### How it works ?

You set destination, and it will calculate easier way to go.

### From Baritone ?

Yes. The Baritone project is open source. I just take the calculation that I need, and keep header/package.

### Some feature are not here !

Yes, block break/place isn't take in count. And I think this will be added soon.

---

## As developer, how can I use ?

You should start the goal to the given direction :

```java
Goal goal = new GoalBlock(x, y, z); // create goal about what you want
BaritoneAPI.getProvider().getNewBaritone(p).getPathingBehavior().startGoal(goal); // start goal
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
Also, there is an event when the path is calculated : `PathCalculatedEvent`. This event is not called when it failed to find path.

## It failed to do what I want !

You can enable debugging :
```java
BaritoneAPI.setPlugin(myPluginInstance); // this is important : used for logger
BaritoneAPI.setDebug(true); // enable debug
```

## Dependencies ?

The alone dependency is spigot. You can use spigot 1.8 to spigot 1.17.

You just have to update the maven project.

## An example ?

This repository include an example located [here](https://github.com/Elikill58/BaritoneCalculator/tree/master/src/com/elikill58). To use it, you have to :
1) Rename `possible_plugin.yml` to `plugin.yml`
2) Rename `possible_config.yml` to `config.yml`
3) Build it yourself

And you will be able to run a plugin that use this dependency. Then, use `/bc` command to try it.

### It will create conflict ?

No. Such as yml files are prefixed with `possible_`, they will not be detected by spigot, and so it will not interact with your files.
