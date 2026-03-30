/**
 * Utility that manages a simple reference count and invokes `set` when the
 * first caller is created and `unset` when the last caller is released.
 *
 * Typical usage:
 * const factory = new UnsetCountCaller(() => showLoader(), () => hideLoader());
 * const release = factory.doSet();
 * // ...later
 * release(); // when the final active caller releases, `unset` is invoked
 */
export class UnsetCountCaller {

    /**
     * Current number of active callers.
     * - Incremented when `doSet` is called.
     * - Decremented when the returned release function is invoked (only the first
     *   invocation of that specific release function has an effect).
     */
    private count: number

    /**
     * Function to call when the first caller is created (count transitions 0 -> 1).
     */
    private readonly set: () => void

    /**
     * Function to call when the last caller is released (count transitions 1 -> 0).
     */
    private readonly unset: () => void

    /**
     * Construct a new UnsetCountCaller.
     *
     * @param set - Called once when the first caller is created.
     * @param unset - Called once when the last caller is released.
     */
    constructor(set: () => void, unset: () => void) {
        this.set = set
        this.unset = unset
        this.count = 0
    }

    /**
     * Create a caller (release function) that releases one reference when invoked.
     *
     * Behavior:
     * - When `doSet` is called and the previous `count` was `0`, `set()` is invoked.
     * - Returns a zero-argument function that, on its first call, decrements the
     *   internal count and, if the count reaches `0`, calls `unset()`.
     * - The returned function is idempotent: subsequent calls have no effect.
     *
     * @returns A release function to decrement the reference count.
     */
    doSet (): () => void {
        if (this.count++ === 0) {
            this.set()
        }

        let isCalled = false;
        return () => {
            // Ensure the caller is only called once
            if(!isCalled) {
                isCalled = true
                this.count--

                if(this.count === 0) {
                    this.unset()
                }
            }
        }
    }
}