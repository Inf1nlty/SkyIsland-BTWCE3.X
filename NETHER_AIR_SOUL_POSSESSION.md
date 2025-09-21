# Nether Air Soul Possession Feature

## Overview
This feature adds a "Nether Air Soul Possession Simulation" mechanism to the SkyIsland mod for Better Than Wolves CE 3.X. When players are in the Nether dimension, they have a chance to randomly trigger soul possession effects that benefit SoulMending mechanics.

## Mechanics
- **Trigger Condition**: Player must be in the Nether dimension (dimensionId == -1)
- **Probability**: 1/1000 chance per tick (~1 in 50 seconds on average at 20 TPS)
- **Server-side Only**: Only processes on the server to avoid client/server desync
- **Effect**: Calls `SoulPossessable.soulMending$onSoulPossession()` which adds 10 soul points to the best totem in the player's inventory and plays possession effects

## Implementation Details
- **Location**: Injected into both `EntityPlayer.onLivingUpdate()` and `EntityPlayerMP.onLivingUpdate()` methods using Mixin
- **Compatibility**: Uses reflection to safely call the SoulMending method, ensuring no crashes if the interface is not available
- **Files Modified**:
  - `src/main/java/com/inf1nlty/skyblock/mixin/world/entity/EntityPlayerMixin.java`
  - `src/main/java/com/inf1nlty/skyblock/mixin/world/entity/EntityPlayerMPMixin.java`

## Code Structure
```java
@Inject(method = "onLivingUpdate", at = @At("HEAD"))
private void netherAirSoulPossession(CallbackInfo ci) {
    EntityPlayer self = (EntityPlayer)(Object)this;
    World world = self.worldObj;
    
    // Only process on server side and in Nether dimension
    if (!world.isRemote && world.provider.dimensionId == -1) {
        // 1/1000 probability per tick
        if (self.rand.nextInt(1000) == 0) {
            // Use reflection to call the SoulMending method
            try {
                java.lang.reflect.Method method = self.getClass().getMethod("soulMending$onSoulPossession");
                method.invoke(self);
            } catch (NoSuchMethodException e) {
                // Silently continue if method not found
            } catch (Exception e) {
                // Log other errors but don't crash
                System.err.println("Warning: Failed to invoke soulMending$onSoulPossession: " + e.getMessage());
            }
        }
    }
}
```

## Integration
This feature integrates seamlessly with existing SoulMending mechanics:
- Does not interfere with normal soul possession triggers
- Uses the same method call as regular possession events
- Maintains compatibility with mods that don't have SoulMending

## Performance Impact
- Minimal performance impact: Only one random number generation and a few conditional checks per tick per player
- Reflection is only used when the random event triggers (very rare)
- No memory leaks or persistent state tracking

## Testing
To test this feature:
1. Enter the Nether dimension
2. Wait for the random trigger (average 50 seconds)
3. Check for soul possession effects (totem soul gain, visual/audio effects)
4. Verify it works in both singleplayer and multiplayer modes