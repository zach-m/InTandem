package tectonica.cindy.framework;

public abstract interface PatchableEntity extends Entity
{
	public void patchWith(Entity partialEntity);
}