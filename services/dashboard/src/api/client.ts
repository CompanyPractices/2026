type FetchApiOptions = {
    timeout?: number;
    retries?: number;
}

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

async function fetchApi<T>(route: string, options: FetchApiOptions = {}): Promise<T> {
    const { timeout = 5000, retries = 2 } = options;
    let lastError: any;

    for (let att = 0; att <= retries; att++) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeout)

        try {
            const response = await fetch(route, { signal: controller.signal});
            clearTimeout(timeoutId);
            if (!response.ok) {
                if (response.status >= 400 && response.status < 500){
                    throw new Error(`Client Error ${response.status}: ${response.statusText}`);
                }
                throw new Error(`Server Error ${response.status}: ${response.statusText}`);
            }
            return await response.json() as T;
        }
        catch (error: any){
            clearTimeout(timeoutId);
            lastError = error;

            if (error.message.startsWith('Client Error')){
                throw error;

            }
            if (error.name === 'AbortError' || error.message.includes('Failed to fetch')) {
                if (att < retries){
                    await wait(2000);
                    continue;
                }
            }

            throw error;
        }
    }

    throw lastError;
}

export default fetchApi;
