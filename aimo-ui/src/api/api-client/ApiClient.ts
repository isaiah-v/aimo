export class ApiClient {
    private readonly baseUrl: string;

    constructor(baseUrl: string) {
        // remove trailing slash(es) if present
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    protected async POST (
        controller: string,
        path: string,
        headers?: HeadersInit,
        body?: any
    ): Promise<Response> {
        return this.request('POST', controller, path, headers, JSON.stringify(body))
    }

    protected async PUT (
        controller: string,
        path: string,
        headers?: HeadersInit,
        body?: any
    ): Promise<Response> {
        return this.request('PUT', controller, path, headers, JSON.stringify(body))
    }

    protected async GET (
        controller: string,
        path: string,
        headers?: HeadersInit
    ): Promise<Response> {
        return this.request('GET', controller, path, headers)
    }

    protected async DELETE (
        controller: string,
        path: string,
        headers?: HeadersInit
    ): Promise<Response> {
        return this.request('DELETE', controller, path, headers)
    }

    protected async request (
        method: string,
        controller: string,
        path: string,
        headers?: HeadersInit,
        body?: BodyInit
    ): Promise<Response> {
        const url = this.createUrl(controller, path)
        return fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json', ...headers },
            body: body
        })
    }

    private createUrl(controller: string, path: string): string {
        return `${this.baseUrl}${controller}${path}`;
    }
}