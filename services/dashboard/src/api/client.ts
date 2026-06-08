function fetchApi<T>(route: string): Promise<T> {
    return fetch(route)
        .then((response) => {
            if (!response.ok) {
                throw new Error(`Error: HTTP ${response.status}: ${response.statusText}`);
            }
            return response.json()
        })
}

export default fetchApi;
