const base_url = import.meta.env.VITE_API_URL;

function fetchApi<T>(route: string, options?: RequestInit): Promise<T> {
    return fetch(base_url + route)
        .then((response) => {
            if (!response.ok) {
                throw new Error(`Error: HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json()
        })
}

export default fetchApi;
