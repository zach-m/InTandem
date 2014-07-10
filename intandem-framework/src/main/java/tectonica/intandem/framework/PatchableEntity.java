package tectonica.intandem.framework;

public abstract interface PatchableEntity extends Entity
{
	public void patchWith(Entity partialEntity);
}